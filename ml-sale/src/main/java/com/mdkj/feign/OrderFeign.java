package com.mdkj.feign;

import com.mdkj.dto.OrderMessage;
import com.mdkj.entity.Order;
import com.mdkj.fallback.OrderFeignFallback;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "ml-order", fallback = OrderFeignFallback.class)
public interface OrderFeign {

    @PostMapping("/api/v1/order/seckillPrePay")
    Result<String> seckillPrePay(@RequestBody OrderMessage orderMessage);

    @GetMapping("/api/v1/order/findUnpaidSn")
    Result<String> findUnpaidSn(@RequestParam("fkUserId") Long fkUserId,
                                @RequestParam("fkCourseId") Long fkCourseId);

    @GetMapping("/api/v1/order/myPage")
    Result<PageVO<Order>> myPage(@RequestParam("fkUserId") Long fkUserId,
                                 @RequestParam("pageNum") Integer pageNum,
                                 @RequestParam("pageSize") Integer pageSize);
}
