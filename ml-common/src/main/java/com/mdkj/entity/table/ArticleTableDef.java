package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 新闻表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class ArticleTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新闻表
     */
    public static final ArticleTableDef ARTICLE = new ArticleTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 序号
     */
    public final QueryColumn IDX = new QueryColumn(this, "idx");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, TITLE, CONTENT, IDX, VERSION, CREATED, UPDATED};

    public ArticleTableDef() {
        super("ml_sms", "article");
    }

    private ArticleTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ArticleTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ArticleTableDef("ml_sms", "article", alias));
    }

}
