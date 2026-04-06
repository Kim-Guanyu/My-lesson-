package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 秒杀表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class SeckillTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 秒杀表
     */
    public static final SeckillTableDef SECKILL = new SeckillTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 0未开始，1已开始，2已结束
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
     * 活动结束时间
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
     * 活动开始时间
     */
    public final QueryColumn START_TIME = new QueryColumn(this, "start_time");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, TITLE, INFO, START_TIME, END_TIME, STATUS, VERSION, CREATED, UPDATED};

    public SeckillTableDef() {
        super("ml_sms", "seckill");
    }

    private SeckillTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public SeckillTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new SeckillTableDef("ml_sms", "seckill", alias));
    }

}
