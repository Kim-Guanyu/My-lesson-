package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 课程表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class CourseTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程表
     */
    public static final CourseTableDef COURSE = new CourseTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 序号
     */
    public final QueryColumn IDX = new QueryColumn(this, "idx");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 封面图地址
     */
    public final QueryColumn COVER = new QueryColumn(this, "cover");

    /**
     * 单价，单位元
     */
    public final QueryColumn PRICE = new QueryColumn(this, "price");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 作者
     */
    public final QueryColumn AUTHOR = new QueryColumn(this, "author");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED = new QueryColumn(this, "created");

    /**
     * 0未删除，1已删除
     */
    public final QueryColumn DELETED = new QueryColumn(this, "deleted");

    /**
     * 摘要图地址
     */
    public final QueryColumn SUMMARY = new QueryColumn(this, "summary");

    /**
     * 修改时间
     */
    public final QueryColumn UPDATED = new QueryColumn(this, "updated");

    /**
     * 数据版本
     */
    public final QueryColumn VERSION = new QueryColumn(this, "version");

    /**
     * 类别ID，类别表外键
     */
    public final QueryColumn FK_CATEGORY_ID = new QueryColumn(this, "fk_category_id");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, TITLE, AUTHOR, FK_CATEGORY_ID, SUMMARY, COVER, PRICE, IDX, INFO, VERSION, CREATED, UPDATED};

    public CourseTableDef() {
        super("ml_cms", "course");
    }

    private CourseTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public CourseTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new CourseTableDef("ml_cms", "course", alias));
    }

}
