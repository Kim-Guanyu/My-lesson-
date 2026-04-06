package com.mdkj.entity;

import com.mybatisflex.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单表")
@Table(value = "order", schema = "ml_oms")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

    /**
     * 编号
     */
    @Schema(description = "编号")
    private String sn;

    /**
     * 总金额
     */
    @Schema(description = "总金额")
    private Double totalAmount;

    /**
     * 实际支付总金额
     */
    @Schema(description = "实际支付总金额")
    private Double payAmount;

    /**
     * 支付方式，0未支付，1微信，2支付宝，3其他
     */
    @Schema(description = "支付方式，0未支付，1微信，2支付宝，3其他")
    private Integer payType;

    /**
     * 订单状态，0未付款，1已付款，2已取消，3其他
     */
    @Schema(description = "订单状态，0未付款，1已付款，2已取消，3其他")
    private Integer status;

    /**
     * 用户ID，用户表外键
     */
    @Schema(description = "用户ID，用户表外键")
    private Long fkUserId;

    /**
     * 用户账号（冗余）
     */
    @Schema(description = "用户账号（冗余）")
    private String username;

    /**
     * 优惠卷ID，优惠卷表外键
     */
    @Schema(description = "优惠卷ID，优惠卷表外键")
    private Long fkCouponsId;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String info;

    /**
     * 数据版本
     */
    @Column(version = true)
    @Schema(description = "数据版本")
    private Long version;

    /**
     * 0未删除，1已删除
     */
    @Column(isLogicDelete = true)
    @Schema(description = "0未删除，1已删除")
    private Integer deleted;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime created;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private LocalDateTime updated;

    /** 1 条订单记录对应 N 条订单明细记录 */
    @RelationOneToMany(selfField = "id", targetField = "fkOrderId")
    private List<OrderDetail> orderDetails;


}
