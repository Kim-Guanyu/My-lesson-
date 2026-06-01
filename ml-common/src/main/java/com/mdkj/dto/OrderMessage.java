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
}
