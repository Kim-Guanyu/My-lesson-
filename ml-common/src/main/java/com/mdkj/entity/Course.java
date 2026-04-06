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
 * 课程表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程表")
@Table(value = "course", schema = "ml_cms")
public class Course implements Serializable {

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
     * 作者
     */
    @Schema(description = "作者")
    private String author;

    /**
     * 类别ID，类别表外键
     */
    @Schema(description = "类别ID，类别表外键")
    private Long fkCategoryId;

    /**
     * 摘要图地址
     */
    @Schema(description = "摘要图地址")
    private String summary;

    /**
     * 封面图地址
     */
    @Schema(description = "封面图地址")
    private String cover;

    /**
     * 单价，单位元
     */
    @Schema(description = "单价，单位元")
    private Double price;

    /**
     * 序号
     */
    @Schema(description = "序号")
    private Long idx;

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

    /** 1 条课程记录对应 1 条课程类别记录 */
    @RelationManyToOne(selfField = "fkCategoryId", targetField = "id")
    private Category category;

    /** 1 条课程记录对应 N 条季次记录 */
    @RelationOneToMany(selfField = "id", targetField = "fkCourseId")
    private List<Season> seasons;


}
