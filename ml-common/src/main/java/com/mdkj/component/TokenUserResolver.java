package com.mdkj.component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.entity.User;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.MyRedis;
import com.mdkj.util.ResultCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 从 Redis 解析登录 Token 对应的用户（与网关校验逻辑一致）
 */
@Component
public class TokenUserResolver {

    @Resource
    private MyRedis redis;

    public User requireUser(String token) {
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(ResultCode.ILLEGAL_PARAM, "请先登录");
        }
        String userJson = redis.get(token);
        if (StrUtil.isBlank(userJson)) {
            throw new ServiceException(ResultCode.ILLEGAL_PARAM, "登录已过期，请重新登录");
        }
        User user = JSONUtil.toBean(userJson, User.class);
        if (user == null || user.getId() == null) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, "用户身份无效");
        }
        return user;
    }
}
