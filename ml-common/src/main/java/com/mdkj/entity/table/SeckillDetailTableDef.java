package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 秒杀明细表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class SeckillDetailTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 秒杀明细表
     */
    public static final SeckillDetailTableDef SECKILL_DETAIL = new SeckillDetailTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED = new QueryColumn(this, "created");

    /**
     * 0未删除，1已删除
     */
    public final QueryColumn DELETED = new QueryColumn(this, "deleted");

    /**
     * 秒杀数量
     */
    public final QueryColumn SK_COUNT = new QueryColumn(this, "sk_count");

    /**
     * 秒杀价格，单位元
     */
    public final QueryColumn SK_PRICE = new QueryColumn(this, "sk_price");

    /**
     * 修改时间
     */
    public final QueryColumn UPDATED = new QueryColumn(this, "updated");

    /**
     * 数据版本
     */
    public final QueryColumn VERSION = new QueryColumn(this, "version");

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
     * 秒杀ID，秒杀表外键
     */
    public final QueryColumn FK_SECKILL_ID = new QueryColumn(this, "fk_seckill_id");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_SECKILL_ID, FK_COURSE_ID, COURSE_TITLE, COURSE_COVER, COURSE_PRICE, SK_PRICE, SK_COUNT, INFO, VERSION, CREATED, UPDATED};

    public SeckillDetailTableDef() {
        super("ml_sms", "seckill_detail");
    }

    private SeckillDetailTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public SeckillDetailTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new SeckillDetailTableDef("ml_sms", "seckill_detail", alias));
    }

}
