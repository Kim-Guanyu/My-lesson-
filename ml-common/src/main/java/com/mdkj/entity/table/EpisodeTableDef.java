package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 集次表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class EpisodeTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 集次表
     */
    public static final EpisodeTableDef EPISODE = new EpisodeTableDef();

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
     * 视频封面地址
     */
    public final QueryColumn COVER = new QueryColumn(this, "cover");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 视频媒体地址
     */
    public final QueryColumn VIDEO = new QueryColumn(this, "video");

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
     * 季次表ID，季次表外键
     */
    public final QueryColumn FK_SEASON_ID = new QueryColumn(this, "fk_season_id");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, TITLE, INFO, VIDEO, COVER, FK_SEASON_ID, IDX, VERSION, CREATED, UPDATED};

    public EpisodeTableDef() {
        super("ml_cms", "episode");
    }

    private EpisodeTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public EpisodeTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new EpisodeTableDef("ml_cms", "episode", alias));
    }

}
