package com.mdkj.component;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.api.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RefreshScope
@Component
public class TokenGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 登录令牌的 Redis key 前缀，必须与 ml-common 的
     * {@code ML.Redis.LOGIN_TOKEN_PREFIX} 保持一致。
     *
     * <p>这里之所以是一份硬编码副本而不是直接引用常量：ml-gateway 没有依赖 ml-common
     * （网关只做路由与鉴权，刻意不引入业务模块）。<b>改动其中一处必须同步改另一处。</b></p>
     */
    private static final String LOGIN_TOKEN_PREFIX = "login:token:";

    /** 令牌有效期（分钟），与 ML.Redis.LOGIN_TOKEN_TTL_MINUTES 保持一致 */
    private static final long LOGIN_TOKEN_TTL_MINUTES = 30L;

    /** 请求白名单：名单中的请求直接放行（从配置中心读取）*/
    @Value("${token.white_list}")
    private List<String> WHITE_LIST;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        // 白名单请求直接放行
        if (isWhite(request)) return chain.filter(exchange);
        // 获取token令牌
        String token = getToken(request);
        // Token令牌不存在
        if (StrUtil.isBlank(token)) {
            return buildResponseData(response, 6000, "登录过期", "请求中未携带Token令牌");
        }
        // 客户端携带的是裸 uuid，服务端拼上前缀才是真正的 Redis key
        String tokenKey = LOGIN_TOKEN_PREFIX + token;
        // 解析Token令牌：查不到即视为未登录/已过期/已被强制下线
        String tokenMessage = stringRedisTemplate.opsForValue().get(tokenKey);
        if (StrUtil.isBlank(tokenMessage)) {
            return buildResponseData(response, 6000, "登录过期", "Redis中不存在该Token令牌");
        }
        // 续期Token令牌：闲置超时而非绝对超时，只要还在操作就不会掉线
        stringRedisTemplate.expire(tokenKey, LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        // 放行请求
        return chain.filter(exchange);
    }

    /**
     * 配置过滤器的优先级
     *
     * @return 值越小，优先级越高
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 构建响应数据
     *
     * @param response     响应对象
     * @param code         响应代码
     * @param message      响应描述
     * @param coderMessage 响应描述（开发人员）
     * @return 响应数据的Mono对象
     */
    private Mono<Void> buildResponseData(ServerHttpResponse response,
                                         int code,
                                         String message,
                                         String coderMessage) {
        // 设置响应类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 设置响应数据（Map）
        Map<String, Object> resultMap = Map.of("code", code, "message", message, "coderMessage", coderMessage);
        // 设置响应数据（Map -> JSON String）
        String resultStr = JSONUtil.toJsonStr(resultMap);
        // 设置响应数据（JSON String -> byte[]）
        byte[] result = resultStr.getBytes(StandardCharsets.UTF_8);
        // 响应
        return response.writeWith(Flux.just(response.bufferFactory().wrap(result)));
    }

    /**
     * 判断请求是否在白名单中
     *
     * @param request 请求对象
     * @return true在白名单中，false不在白名单中
     */
    private boolean isWhite(ServerHttpRequest request) {
        boolean result = false;
        String url = request.getURI().toString();
        for (String white : WHITE_LIST) {
            if (url.contains(white)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 依次尝试从请求头和请求参数中获取Token令牌
     *
     * @param request 请求对象
     * @return 获取成功返回token令牌，获取失败返回null
     */
    private String getToken(ServerHttpRequest request) {
        // 尝试从请求头中获取Token令牌
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isBlank(token)) {
            // 尝试从查询串中获取Token令牌
            token = request.getQueryParams().getFirst("token");
        }
        return token;
    }
}
