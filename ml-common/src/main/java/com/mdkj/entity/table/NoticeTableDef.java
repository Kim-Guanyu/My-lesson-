package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 通知表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class NoticeTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知表
     */
    public static final NoticeTableDef NOTICE = new NoticeTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 序号
     */
    public final QueryColumn IDX = new QueryColumn(this, "idx");

    /**
     * 内容
     */
    public final QueryColumn CONTENT = new QueryColumn(this, "content");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, CONTENT, IDX, VERSION, CREATED, UPDATED};

    public NoticeTableDef() {
        super("ml_sms", "notice");
    }

    private NoticeTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public NoticeTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new NoticeTableDef("ml_sms", "notice", alias));
    }

}
