package com.mdkj.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class OrderMessage implements Serializable {
    /** 预生成的订单编号，MQ 消费端按此编号建单 */
    private String sn;
    private Long fkSeckillId;
    private Long fkUserId;
    private Long fkCourseId;
    private Double price;
    private Double skPrice;
    /**
     * 以下字段由 ml-sale 在生成消息时直接带上（下单时已持有对应数据），
     * 目的是让 ml-order 消费端建单时无需再同步调用 user/course 微服务，
     * 减少秒杀订单创建链路上的远程调用与等待耗时。
     */
    private String username;
    private String courseTitle;
    private String courseCover;
}
