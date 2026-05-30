package com.mdkj.fallback;

import com.mdkj.dto.OrderMessage;
import com.mdkj.entity.Order;
import com.mdkj.feign.OrderFeign;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFeignFallback implements OrderFeign {

    @Override
    public Result<String> seckillPrePay(OrderMessage orderMessage) {
        log.error("订单微服务远程调用失败，请联系管理员。");
        return null;
    }

    @Override
    public Result<String> findUnpaidSn(Long fkUserId, Long fkCourseId) {
        log.error("订单微服务远程调用失败，请联系管理员。");
        return null;
    }

    @Override
    public Result<PageVO<Order>> myPage(Long fkUserId, Integer pageNum, Integer pageSize) {
        log.error("订单微服务远程调用失败，请联系管理员。");
        return null;
    }
}
