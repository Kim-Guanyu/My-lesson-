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
 * 菜单表 实体类。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜单表")
@Table(value = "menu", schema = "ml_ums")
public class Menu implements Serializable {

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
     * 跳转地址
     */
    @Schema(description = "跳转地址")
    private String url;

    /**
     * 图标名称
     */
    @Schema(description = "图标名称")
    private String icon;

    /**
     * 父菜单主键，0视为根节点
     */
    @Schema(description = "父菜单主键，0视为根节点")
    private Long pid;

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

    /** 1 条菜单记录对应 1 条父菜单记录 */
    @RelationOneToOne(selfField = "pid", targetField = "id")
    private Menu parentMenu;

    /** 1 条菜单记录对应 N 条子菜单记录 */
    @RelationOneToMany(selfField = "id", targetField = "pid", orderBy = "idx")
    private List<Menu> subMenus;


}
