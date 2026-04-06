package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 订单明细表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class OrderDetailTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单明细表
     */
    public static final OrderDetailTableDef ORDER_DETAIL = new OrderDetailTableDef();

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
     * 订单ID，订单表外键
     */
    public final QueryColumn FK_ORDER_ID = new QueryColumn(this, "fk_order_id");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_ORDER_ID, FK_COURSE_ID, COURSE_TITLE, COURSE_COVER, COURSE_PRICE, VERSION, CREATED, UPDATED};

    public OrderDetailTableDef() {
        super("ml_oms", "order_detail");
    }

    private OrderDetailTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public OrderDetailTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new OrderDetailTableDef("ml_oms", "order_detail", alias));
    }

}
