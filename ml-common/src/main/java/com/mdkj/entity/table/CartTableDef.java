package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 购物车表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class CartTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 购物车表
     */
    public static final CartTableDef CART = new CartTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

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
     * 用户ID，用户表外键
     */
    public final QueryColumn FK_USER_ID = new QueryColumn(this, "fk_user_id");

    /**
     * 用户账号（冗余）
     */
    public final QueryColumn USERNAME = new QueryColumn(this, "username");

    /**
     * 课程ID，课程表外键
     */
    public final QueryColumn FK_COURSE_ID = new QueryColumn(this, "fk_course_id");

    /**
     * 课程封面图（冗余）
     */
    public final QueryColumn COURSE_COVER = new QueryColumn(this, "course_cover");

    /**
     * 课程单价，单位元（冗余）
     */
    public final QueryColumn COURSE_PRICE = new QueryColumn(this, "course_price");

    /**
     * 课程标题（冗余）
     */
    public final QueryColumn COURSE_TITLE = new QueryColumn(this, "course_title");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_USER_ID, USERNAME, FK_COURSE_ID, COURSE_TITLE, COURSE_COVER, COURSE_PRICE, VERSION, CREATED, UPDATED};

    public CartTableDef() {
        super("ml_oms", "cart");
    }

    private CartTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public CartTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new CartTableDef("ml_oms", "cart", alias));
    }

}
