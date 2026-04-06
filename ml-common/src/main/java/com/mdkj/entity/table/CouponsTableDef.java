package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 优惠卷表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class CouponsTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 优惠卷表
     */
    public static final CouponsTableDef COUPONS = new CouponsTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 兑换码
     */
    public final QueryColumn CODE = new QueryColumn(this, "code");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 优惠金额，单位分
     */
    public final QueryColumn CP_PRICE = new QueryColumn(this, "cp_price");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED = new QueryColumn(this, "created");

    /**
     * 0未删除，1已删除
     */
    public final QueryColumn DELETED = new QueryColumn(this, "deleted");

    /**
     * 优惠卷失效时间
     */
    public final QueryColumn END_TIME = new QueryColumn(this, "end_time");

    /**
     * 修改时间
     */
    public final QueryColumn UPDATED = new QueryColumn(this, "updated");

    /**
     * 数据版本
     */
    public final QueryColumn VERSION = new QueryColumn(this, "version");

    /**
     * 优惠卷生效时间
     */
    public final QueryColumn START_TIME = new QueryColumn(this, "start_time");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, CODE, TITLE, CP_PRICE, INFO, START_TIME, END_TIME, VERSION, CREATED, UPDATED};

    public CouponsTableDef() {
        super("ml_sms", "coupons");
    }

    private CouponsTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public CouponsTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new CouponsTableDef("ml_sms", "coupons", alias));
    }

}
