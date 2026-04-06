package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 菜单表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class MenuTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单表
     */
    public static final MenuTableDef MENU = new MenuTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 序号
     */
    public final QueryColumn IDX = new QueryColumn(this, "idx");

    /**
     * 父菜单主键，0视为根节点
     */
    public final QueryColumn PID = new QueryColumn(this, "pid");

    /**
     * 跳转地址
     */
    public final QueryColumn URL = new QueryColumn(this, "url");

    /**
     * 图标名称
     */
    public final QueryColumn ICON = new QueryColumn(this, "icon");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED = new QueryColumn(this, "created");

    /**
     * 0未删除，1已删除
     */
    public final QueryColumn DELETED = new QueryColumn(this, "deleted");

    /**
     * 修改时间
     */
    public final QueryColumn UPDATED = new QueryColumn(this, "updated");

    /**
     * 数据版本
     */
    public final QueryColumn VERSION = new QueryColumn(this, "version");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, TITLE, URL, ICON, PID, IDX, INFO, VERSION, CREATED, UPDATED};

    public MenuTableDef() {
        super("ml_ums", "menu");
    }

    private MenuTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public MenuTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new MenuTableDef("ml_ums", "menu", alias));
    }

}
