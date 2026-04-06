package com.mdkj.component;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.dao.BarrageRepository;
import com.mdkj.es.BarrageDoc;
import com.mdkj.util.SpringUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@ServerEndpoint("/api/v1/barrage/{userId}")
public class BarrageServer {

    /** 雪花算法（全局唯一）*/
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    /** 静态线程池（全局唯一，避免每次创建销毁，核心线程数根据业务调整）*/
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(10);

    /** 用于存储每个WS客户端的Session实例: ConcurrentHashMap保证线程安全，static保证实例唯一 */
    private static final Map<Long, Session> CLIENTS = new ConcurrentHashMap<>();

    /**
     * 当WS客户端上线时触发
     *
     * @param userId  WS客户端唯一标识
     * @param session 每个WS客户端连入WS服务端时，都会生成独有的session对象
     */
    @OnOpen
    public void onOpen(@PathParam("userId") Long userId, Session session) {
        // 将客户端唯一标识及其Session实例存入Map
        CLIENTS.put(userId, session);
        log.info("{} 号用户上线了", userId);
    }

    /**
     * 当WS客户端下线时触发
     *
     * @param userId  WS客户端唯一标识
     * @param session 每个WS客户端连入WS服务端时，都会生成独有的session对象
     */
    @OnClose
    public void onClose(@PathParam("userId") Long userId, Session session) {
        // 从Map中移除该客户端和及其Session实例
        CLIENTS.remove(userId);
        log.info("{} 号用户下线了", userId);
    }

    /**
     * 当WS客户端连接WS服务端异常时触发
     *
     * @param userId  WS客户端唯一标识
     * @param e       异常实例
     * @param session 每个WS客户端连入WS服务端时，都会生成独有的session对象
     */
    @OnError
    public void onError(@PathParam("userId") Long userId, Throwable e, Session session) {
        log.error("{} 号用户连接或者通信异常", userId, e);
    }

    /**
     * 当WS服务端接收到弹幕消息的时候触发: 对Map中的全部在线的WS客户端群发弹幕消息
     *
     * @param userId  WS客户端唯一标识
     * @param msg     弹幕消息内容
     * @param session 每个WS客户端连入WS服务端时，都会生成独有的session对象
     */
    @OnMessage
    public void onMessage(@PathParam("userId") Long userId, String msg, Session session) {

		log.info("{} 号用户发来弹幕: {}", userId, msg);

        // 群发消息（同步快速处理，不阻塞）
        CLIENTS.forEach((key, clientSession) -> {
            if (clientSession.isOpen()) {
                clientSession.getAsyncRemote().sendText(msg);
            }
        });

        // 使用静态线程池异步将弹幕消息存入 ES 数据库
        ASYNC_EXECUTOR.execute(() -> {
            try {
                // 将 JSON 格式的消息转换为 BarrageDoc 类型的消息
                BarrageDoc barrageDoc = JSONUtil.toBean(msg, BarrageDoc.class);
                // 手动生成 ID（雪花算法）
                barrageDoc.setId(SNOWFLAKE.nextIdStr());
                // 动态获取 BarrageRepository（通过 SpringContextUtil）
                BarrageDoc result = SpringUtil.getBean(BarrageRepository.class).save(barrageDoc);
                log.info("{} 号用户的弹幕保存成功，弹幕ID：{}", userId, result.getId());
            } catch (Exception e) {
                log.error("{} 号用户的弹幕保存失败，消息内容：{}", userId, msg, e);
            }

        });
        log.info("{} 号用户发来弹幕: {}", userId, msg);
    }
}
