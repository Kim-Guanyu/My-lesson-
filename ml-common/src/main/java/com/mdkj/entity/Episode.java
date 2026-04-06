package com.mdkj.entity;

import com.mybatisflex.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 集次表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集次表")
@Table(value = "episode", schema = "ml_cms")
public class Episode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String info;

    /**
     * 视频媒体地址
     */
    @Schema(description = "视频媒体地址")
    private String video;

    /**
     * 视频封面地址
     */
    @Schema(description = "视频封面地址")
    private String cover;

    /**
     * 季次表ID，季次表外键
     */
    @Schema(description = "季次表ID，季次表外键")
    private Long fkSeasonId;

    /**
     * 序号
     */
    @Schema(description = "序号")
    private Long idx;

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

    /** 1 条集次记录对应 1 条季次记录 */
    @RelationManyToOne(selfField = "fkSeasonId", targetField = "id")
    private Season season;


}
