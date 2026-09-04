package com.mdkj.component;

import cn.hutool.core.util.StrUtil;
import com.mdkj.entity.User;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.ResultCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 从 Redis 解析登录 Token 对应的用户（与网关校验逻辑一致）。
 *
 * <p>Key 规范与读取细节已下沉到 {@link LoginTokenStore}，本类只负责
 * 「解析不到就抛业务异常」这层语义，方便业务代码直接用。</p>
 */
@Component
public class TokenUserResolver {

    @Resource
    private LoginTokenStore loginTokenStore;

    /**
     * 解析令牌并要求必须有效，否则抛出业务异常。
     *
     * @param token 客户端携带的裸令牌
     * @return 登录用户
     */
    public User requireUser(String token) {
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(ResultCode.ILLEGAL_PARAM, "请先登录");
        }
        // 令牌 key 的前缀拼装、JSON 反序列化都在 LoginTokenStore 内部完成
        User user = loginTokenStore.resolve(token);
        if (user == null) {
            throw new ServiceException(ResultCode.ILLEGAL_PARAM, "登录已过期，请重新登录");
        }
        if (user.getId() == null) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, "用户身份无效");
        }
        return user;
    }
}
