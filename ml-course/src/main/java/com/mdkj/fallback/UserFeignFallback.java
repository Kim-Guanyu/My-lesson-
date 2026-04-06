package com.mdkj.fallback;

import com.alibaba.nacos.api.model.v2.Result;
import com.mdkj.entity.User;
import com.mdkj.feign.UserFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignFallback implements UserFeign {

    @Override
    public Result<User> select(Long id) {
        log.error("用户微服务远程调用失败，请联系管理员。");
        return null;
    }
}
