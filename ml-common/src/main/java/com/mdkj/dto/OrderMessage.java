package com.mdkj.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class OrderMessage implements Serializable {
    private Long fkUserId;
    private Long fkCourseId;
    private Double price;
    private Double skPrice;
}
