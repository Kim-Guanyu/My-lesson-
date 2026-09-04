package com.mdkj.service.impl;

import com.mdkj.component.LoginTokenStore;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.UserRole;
import com.mdkj.mapper.UserRoleMapper;
import com.mdkj.service.UserRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 用户角色关系表 服务层实现。
 *
 * <p>本类新增的三个方法都在通用 CRUD 之外多做一件事：
 * <b>用户的角色一旦变动，就强制他重新登录。</b></p>
 *
 * <p>原因是权限数据（roleTitles / menus）是在登录时一次性算好塞进 LoginVO 的
 * （见 {@code UserServiceImpl#buildLoginVO}），前端据此渲染菜单、注册动态路由。
 * 因此改了 user_role 而不清登录态的话，用户手里仍是旧权限：刚被撤销的角色，
 * 他照样能继续用，直到令牌自然过期——而令牌每次请求都在续期，只要他不停操作
 * 就可以无限期不过期。</p>
 *
 * <p>这里刻意<b>不覆盖</b>父类的 {@code save/updateById/removeById}，而是提供
 * 语义明确的新方法，好处是不与框架的方法签名耦合；代价是调用方必须记得用新方法，
 * 所以接口注释里做了显式提示。</p>
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>  implements UserRoleService{

    @Resource
    private LoginTokenStore loginTokenStore;

    @Override
    public boolean saveAndKickUser(UserRole userRole) {
        boolean saved = this.save(userRole);
        if (saved) {
            // 新角色要生效，必须让他重新登录去重算一遍 roleTitles / menus
            loginTokenStore.invalidateByUserId(userRole.getFkUserId());
        }
        return saved;
    }

    @Override
    public boolean updateAndKickUser(UserRole userRole) {
        // 先按主键取出旧记录：如果这次修改把关系从 A 用户改到了 B 用户，
        // 那么 A 失去角色、B 获得角色，两个人的登录态都过时了
        UserRole old = userRole.getId() == null ? null : this.getById(userRole.getId());

        boolean updated = this.updateById(userRole);
        if (updated) {
            if (old != null) {
                loginTokenStore.invalidateByUserId(old.getFkUserId());
            }
            // 只有当新属主与旧属主不同才需要再踢一次，相同的话上面已经踢过了
            Long newUserId = userRole.getFkUserId();
            if (newUserId != null && (old == null || !newUserId.equals(old.getFkUserId()))) {
                loginTokenStore.invalidateByUserId(newUserId);
            }
        }
        return updated;
    }

    @Override
    public boolean removeByIdAndKickUser(Long id) {
        // 顺序很关键：记录一删就查不到属主了，所以 fkUserId 必须在删之前拿到
        UserRole old = this.getById(id);
        boolean removed = this.removeById(id);
        if (removed && old != null) {
            loginTokenStore.invalidateByUserId(old.getFkUserId());
        }
        return removed;
    }
}
