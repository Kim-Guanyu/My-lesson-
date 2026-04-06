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
 * 角色菜单关系表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色菜单关系表")
@Table(value = "role_menu", schema = "ml_ums")
public class RoleMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键")
    private Long id;

    /**
     * 角色ID，角色表外键
     */
    @Schema(description = "角色ID，角色表外键")
    private Long fkRoleId;

    /**
     * 菜单ID，菜单表外键
     */
    @Schema(description = "菜单ID，菜单表外键")
    private Long fkMenuId;

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

    /** 1 条 RoleMenu 中间表记录对应 1 条菜单记录 */
    @RelationOneToOne(selfField = "fkMenuId", targetField = "id")
    private Menu menu;

    /** 1 条 RoleMenu 中间表记录对应 1 条角色记录 */
    @RelationOneToOne(selfField = "fkRoleId", targetField = "id")
    private Role role;


}
