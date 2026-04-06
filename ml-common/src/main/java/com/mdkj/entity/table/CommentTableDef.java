package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 评论表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class CommentTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论表
     */
    public static final CommentTableDef COMMENT = new CommentTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 父评论主键，0视为根节点
     */
    public final QueryColumn PID = new QueryColumn(this, "pid");

    /**
     * 评论人头像，冗余
     */
    public final QueryColumn AVATAR = new QueryColumn(this, "avatar");

    /**
     * 评论内容
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
     * 评论人ID，用户表外键
     */
    public final QueryColumn FK_USER_ID = new QueryColumn(this, "fk_user_id");

    /**
     * 评论人昵称，冗余
     */
    public final QueryColumn NICKNAME = new QueryColumn(this, "nickname");

    /**
     * 评论人省份，冗余
     */
    public final QueryColumn PROVINCE = new QueryColumn(this, "province");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_EPISODE_ID, FK_USER_ID, NICKNAME, AVATAR, PROVINCE, PID, CONTENT, VERSION, CREATED, UPDATED};

    public CommentTableDef() {
        super("ml_cms", "comment");
    }

    private CommentTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public CommentTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new CommentTableDef("ml_cms", "comment", alias));
    }

}
