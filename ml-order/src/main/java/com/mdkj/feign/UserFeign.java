package com.mdkj.feign;

import com.mdkj.entity.User;
import com.mdkj.fallback.UserFeignFallback;
import com.mdkj.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "ml-user", fallback = UserFeignFallback.class)
public interface UserFeign {

    @GetMapping("/api/v1/user/select/{id}")
    Result<User> select(@PathVariable("id") Long id);
}
