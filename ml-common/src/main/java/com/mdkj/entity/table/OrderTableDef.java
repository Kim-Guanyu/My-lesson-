package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 订单表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class OrderTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单表
     */
    public static final OrderTableDef ORDER = new OrderTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 编号
     */
    public final QueryColumn SN = new QueryColumn(this, "sn");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 订单状态，0未付款，1已付款，2已取消，3其他
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED = new QueryColumn(this, "created");

    /**
     * 0未删除，1已删除
     */
    public final QueryColumn DELETED = new QueryColumn(this, "deleted");

    /**
     * 支付方式，0未支付，1微信，2支付宝，3其他
     */
    public final QueryColumn PAY_TYPE = new QueryColumn(this, "pay_type");

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
     * 实际支付总金额
     */
    public final QueryColumn PAY_AMOUNT = new QueryColumn(this, "pay_amount");

    /**
     * 优惠卷ID，优惠卷表外键
     */
    public final QueryColumn FK_COUPONS_ID = new QueryColumn(this, "fk_coupons_id");

    /**
     * 总金额
     */
    public final QueryColumn TOTAL_AMOUNT = new QueryColumn(this, "total_amount");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, SN, TOTAL_AMOUNT, PAY_AMOUNT, PAY_TYPE, STATUS, FK_USER_ID, USERNAME, FK_COUPONS_ID, INFO, VERSION, CREATED, UPDATED};

    public OrderTableDef() {
        super("ml_oms", "order");
    }

    private OrderTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public OrderTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new OrderTableDef("ml_oms", "order", alias));
    }

}
