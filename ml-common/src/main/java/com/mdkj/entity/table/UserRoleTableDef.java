package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 用户角色关系表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class UserRoleTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户角色关系表
     */
    public static final UserRoleTableDef USER_ROLE = new UserRoleTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

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
     * 角色ID，角色表外键
     */
    public final QueryColumn FK_ROLE_ID = new QueryColumn(this, "fk_role_id");

    /**
     * 用户ID，用户表外键
     */
    public final QueryColumn FK_USER_ID = new QueryColumn(this, "fk_user_id");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, FK_USER_ID, FK_ROLE_ID, VERSION, CREATED, UPDATED};

    public UserRoleTableDef() {
        super("ml_ums", "user_role");
    }

    private UserRoleTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public UserRoleTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new UserRoleTableDef("ml_ums", "user_role", alias));
    }

}
