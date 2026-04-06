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
 * 通知表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知表")
@Table(value = "notice", schema = "ml_sms")
public class Notice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

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

}
