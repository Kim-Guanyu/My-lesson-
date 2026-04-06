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
 * 举报表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "举报表")
@Table(value = "report", schema = "ml_cms")
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

    /**
     * 集次ID，集次表外键
     */
    @Schema(description = "集次ID，集次表外键")
    private Long fkEpisodeId;

    /**
     * 举报人ID，用户表外键
     */
    @Schema(description = "举报人ID，用户表外键")
    private Long fkUserId;

    /**
     * 举报人昵称，冗余
     */
    @Schema(description = "举报人昵称，冗余")
    private String nickname;

    /**
     * 举报内容
     */
    @Schema(description = "举报内容")
    private String content;

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

    /** N 条评论记录对应 1 条集次（视频）记录 */
    @RelationManyToOne(selfField = "fkEpisodeId", targetField = "id")
    private Episode episode;


}
