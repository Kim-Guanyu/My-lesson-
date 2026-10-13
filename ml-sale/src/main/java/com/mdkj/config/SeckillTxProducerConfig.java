package com.mdkj.config;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀事务消息专用的生产者与 RocketMQTemplate。
 *
 * <p><b>为什么不复用自动装配的 rocketMQTemplate</b>：事务消息在提交/回滚的网络往返丢失时，
 * 依赖 broker 反向「回查」生产者来裁决本地事务状态。broker 是按 <b>producer group</b>
 * 找回查对象的（{@code ProducerManager#getAvailableChannel} 用
 * {@code counter.incrementAndGet() % size} 在该组的所有 channel 里轮询挑一个）。
 * 而 ml-sale 与 ml-order 共用配置中心里的 {@code rocketmq.producer.group}，
 * 若沿用同一个组，回查请求有概率落到没有事务监听器的 ml-order 实例上 ——
 * 客户端发现没有监听器就只打一行 warn 后直接返回，这一轮回查被静默吞掉，
 * 白白消耗一次 {@code transactionCheckMax} 配额。配额耗尽（默认 15 次）后
 * broker 会丢弃半消息，变成「库存已扣、订单永不创建」的永久少卖。
 * 独立成组后回查必然命中本服务。</p>
 *
 * <p>回查落到哪个 ml-sale 实例都是正确的：判据是共享 Redis 里的占位 Key，与实例无关，
 * 因此本服务可以自由横向扩容，不需要粘性路由。</p>
 *
 * <p><b>约束</b>：本类会让容器里存在两个 {@link RocketMQTemplate} Bean，
 * 此后 ml-sale 内<b>任何按类型注入 {@code RocketMQTemplate} 的代码都会启动失败</b>
 * （{@code NoUniqueBeanDefinitionException}）。必须让字段名与 Bean 名逐字符一致，
 * 或显式写 {@code @Resource(name = TX_TEMPLATE_BEAN)}。
 * 另外 producer 只能是本方法内的局部变量，<b>绝不能暴露成 Bean</b> ——
 * 容器里一旦出现任意 {@code DefaultMQProducer} 类型的 Bean，
 * 自动装配的 {@code defaultMQProducer} 就不再创建，默认 Template 会失去 producer 变成哑弹。</p>
 *
 * @author Kim-Guanyu
 * @since v2.1.0
 */
@Configuration
public class SeckillTxProducerConfig {

    /** 事务消息生产者组，必须与 ml-order 使用的组区分开 */
    public static final String TX_PRODUCER_GROUP = "ml-sale-seckill-tx-group";

    /** 事务消息 Template 的 Bean 名，供 {@code @RocketMQTransactionListener} 与注入点引用 */
    public static final String TX_TEMPLATE_BEAN = "seckillTxRocketMQTemplate";

    /**
     * 事务消息专用 Template。
     *
     * <p>{@link RocketMQTemplate} 实现了 InitializingBean / DisposableBean，
     * 容器会自动 start / shutdown 内部的 producer，无需手动管理生命周期。
     * 事务监听器由 {@code RocketMQTransactionConfiguration} 按 Bean 名反查后
     * 调用 {@code setTransactionListener} 挂上，这里不需要（也不能）自己设置。</p>
     */
    @Bean(TX_TEMPLATE_BEAN)
    public RocketMQTemplate seckillTxRocketMQTemplate(RocketMQProperties properties,
                                                      RocketMQMessageConverter messageConverter) {
        // 必须是 TransactionMQProducer，RocketMQTransactionConfiguration 会对它做强制类型转换
        TransactionMQProducer producer = new TransactionMQProducer(TX_PRODUCER_GROUP);
        producer.setNamesrvAddr(properties.getNameServer());
        if (StrUtil.isNotBlank(properties.getAccessChannel())) {
            producer.setAccessChannel(AccessChannel.valueOf(properties.getAccessChannel()));
        }

        // 其余发送参数沿用配置中心里的 rocketmq.producer.*，与默认 Template 行为保持一致
        RocketMQProperties.Producer producerConfig = properties.getProducer();
        if (producerConfig != null) {
            producer.setSendMsgTimeout(producerConfig.getSendMessageTimeout());
            producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
            producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
            producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
            producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMessageBodyThreshold());
            producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryNextServer());
        }

        // 这里是设置回查线程池的唯一有效时机：TransactionMQProducer.start() 会在
        // initTransactionEnv() 里把 executorService 一次性快照进 checkExecutor，
        // 而 @RocketMQTransactionListener 上的 corePoolSize 等参数是由
        // RocketMQTransactionConfiguration（SmartInitializingSingleton）在所有 Bean
        // 初始化完成、producer 早已 start 之后才 set 的，设了也读不到。
        // 不设则回落到默认的 1 核 1 线程。
        producer.setExecutorService(new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                runnable -> new Thread(runnable, "seckill-tx-check")));

        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        template.setMessageConverter(messageConverter.getMessageConverter());
        return template;
    }
}
