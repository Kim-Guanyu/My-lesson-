package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 举报表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class ReportTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报表
     */
    public static final ReportTableDef REPORT = new ReportTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 举报内容
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
     * 举报人ID，用户表外键
     */
    public final QueryColumn FK_USER_ID = new QueryColumn(this, "fk_user_id");

    /**
     * 举报人昵称，冗余
     */
    public final QueryColumn NICKNAME = new QueryColumn(this, "nickname");

    /**
     * 集次ID，集次表外键
     */
    public final QueryColumn FK_EPISODE_ID = new QueryColumn(this, "fk_episode_id");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_EPISODE_ID, FK_USER_ID, NICKNAME, CONTENT, VERSION, CREATED, UPDATED};

    public ReportTableDef() {
        super("ml_cms", "report");
    }

    private ReportTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ReportTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ReportTableDef("ml_cms", "report", alias));
    }

}
