package com.mdkj.service;

import com.mybatisflex.core.service.IService;
import com.mdkj.entity.UserRole;

/**
 * 用户角色关系表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 给用户新增角色，并强制该用户重新登录。
     *
     * <p>权限数据（roleTitles / menus）是登录时一次性算好塞进 LoginVO 的，
     * 所以只改 user_role 表不清登录态的话，用户手里仍是旧权限。
     * <b>请用本方法代替通用的 {@code save}。</b></p>
     *
     * @param userRole 用户角色关系
     * @return {@code true} 新增成功
     */
    boolean saveAndKickUser(UserRole userRole);

    /**
     * 修改用户角色关系，并强制相关用户重新登录。
     *
     * <p>若本次修改把关系从 A 用户改到了 B 用户，A、B 两人的登录态都会被清掉。
     * <b>请用本方法代替通用的 {@code updateById}。</b></p>
     *
     * @param userRole 用户角色关系（需带主键）
     * @return {@code true} 修改成功
     */
    boolean updateAndKickUser(UserRole userRole);

    /**
     * 撤销用户角色，并强制该用户重新登录。
     *
     * <p><b>请用本方法代替通用的 {@code removeById}。</b></p>
     *
     * @param id 用户角色关系主键
     * @return {@code true} 删除成功
     */
    boolean removeByIdAndKickUser(Long id);
}
