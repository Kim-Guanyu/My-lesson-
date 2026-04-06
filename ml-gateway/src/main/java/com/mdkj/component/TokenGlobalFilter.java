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
        // 解析Token令牌
        String tokenMessage = stringRedisTemplate.opsForValue().get(token);
        if (StrUtil.isBlank(tokenMessage)) {
            return buildResponseData(response, 6000, "登录过期", "Redis中不存在该Token令牌");
        }
        // 续期Token令牌
        stringRedisTemplate.expire(token, 30, TimeUnit.MINUTES);
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
