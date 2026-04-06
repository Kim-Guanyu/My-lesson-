package com.mdkj.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车表")
@Table(value = "cart", schema = "ml_oms")
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

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
     * 课程ID，课程表外键
     */
    @Schema(description = "课程ID，课程表外键")
    private Long fkCourseId;

    /**
     * 课程标题（冗余）
     */
    @Schema(description = "课程标题（冗余）")
    private String courseTitle;

    /**
     * 课程封面图（冗余）
     */
    @Schema(description = "课程封面图（冗余）")
    private String courseCover;

    /**
     * 课程单价，单位元（冗余）
     */
    @Schema(description = "课程单价，单位元（冗余）")
    private Double coursePrice;

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

}
