package com.mdkj.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 用户表 表定义层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public class UserTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户表
     */
    public static final UserTableDef USER = new UserTableDef();

    /**
     * 主键
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 年龄
     */
    public final QueryColumn AGE = new QueryColumn(this, "age");

    /**
     * 描述
     */
    public final QueryColumn INFO = new QueryColumn(this, "info");

    /**
     * 邮箱
     */
    public final QueryColumn EMAIL = new QueryColumn(this, "email");

    /**
     * 手机
     */
    public final QueryColumn PHONE = new QueryColumn(this, "phone");

    /**
     * 头像
     */
    public final QueryColumn AVATAR = new QueryColumn(this, "avatar");

    /**
     * 性别，0女，1男，2保密
     */
    public final QueryColumn GENDER = new QueryColumn(this, "gender");

    /**
     * 身份证号
     */
    public final QueryColumn IDCARD = new QueryColumn(this, "idcard");

    /**
     * 星座
     */
    public final QueryColumn ZODIAC = new QueryColumn(this, "zodiac");

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
     * 昵称
     */
    public final QueryColumn NICKNAME = new QueryColumn(this, "nickname");

    /**
     * 密码
     */
    public final QueryColumn PASSWORD = new QueryColumn(this, "password");

    /**
     * 省份
     */
    public final QueryColumn PROVINCE = new QueryColumn(this, "province");

    /**
     * 姓名
     */
    public final QueryColumn REALNAME = new QueryColumn(this, "realname");

    /**
     * 账号
     */
    public final QueryColumn USERNAME = new QueryColumn(this, "username");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USERNAME, PASSWORD, NICKNAME, EMAIL, PROVINCE, REALNAME, AVATAR, ZODIAC, PHONE, IDCARD, GENDER, AGE, INFO, VERSION, CREATED, UPDATED};

    public UserTableDef() {
        super("ml_ums", "user");
    }

    private UserTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public UserTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new UserTableDef("ml_ums", "user", alias));
    }

}
