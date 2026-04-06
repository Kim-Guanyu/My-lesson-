package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.RoleInsertDTO;
import com.mdkj.dto.RolePageDTO;
import com.mdkj.dto.RoleSimpleListVO;
import com.mdkj.dto.RoleUpdateDTO;
import com.mdkj.entity.UserRole;
import com.mdkj.exception.ServiceException;
import com.mdkj.mapper.RoleMenuMapper;
import com.mdkj.mapper.UserRoleMapper;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Role;
import com.mdkj.mapper.RoleMapper;
import com.mdkj.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mdkj.entity.table.RoleMenuTableDef.ROLE_MENU;
import static com.mdkj.entity.table.RoleTableDef.ROLE;
import static com.mdkj.entity.table.UserRoleTableDef.USER_ROLE;

/**
 * 角色表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>  implements RoleService{

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Override
    public boolean insert(RoleInsertDTO dto) {
        String title = dto.getTitle();

        // 标题查重
        // select count(*) from role where title = ?
        if (QueryChain.of(mapper)
                .where(ROLE.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "角色标题" + title + "已存在");
        }

        // 组装实体类
        Role role = BeanUtil.copyProperties(dto, Role.class);
        role.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        role.setCreated(LocalDateTime.now());
        role.setUpdated(LocalDateTime.now());

        // insert into role (title, info, idx, created, updated) values (?, ?, ?, ?, ?)
        if (mapper.insert(role) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Role select(Long id) {
        // select * from role where id = ?
        Role role = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(role)) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, id + "号角色数据不存在");
        }
        return role;
    }

    @Override
    public List<RoleSimpleListVO> simpleList() {
        // select * from role order by idx asc, id desc
        return QueryChain.of(mapper)
                .orderBy(ROLE.IDX.asc(), ROLE.ID.desc())
                .withRelations()
                .listAs(RoleSimpleListVO.class);
    }

    @Override
    public PageVO<Role> page(RolePageDTO dto) {

        // select * from role order by idx asc, id desc
        QueryChain<Role> queryChain = QueryChain.of(mapper)
                .orderBy(ROLE.IDX.asc(), ROLE.ID.desc());

        // title条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(ROLE.TITLE.like(title));
        }

        // DB分页并转为VO
        Page<Role> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Role> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(RoleUpdateDTO dto) {
        String title = dto.getTitle();

        // 检查角色是否存在
        this.existsById(dto.getId());

        // 标题查重
        // select count(*) from role where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(ROLE.TITLE.eq(title))
                .and(ROLE.ID.ne(dto.getId()))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "角色标题" + title + "已存在");
        }

        // 组装实体类
        Role role = BeanUtil.copyProperties(dto, Role.class);
        role.setUpdated(LocalDateTime.now());

        // update role set title = ?, info = ?, idx = ?, updated = ? where id = ?
        if (!UpdateChain.of(role)
                .where(ROLE.ID.eq(role.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id) {

        // 检查角色是否存在
        this.existsById(id);

        // 删除中间表
        // delete from role_menu where fk_role_id = ?
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.eq(id))
                .remove();
        // delete from user_role where fk_role_id = ?
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_ROLE_ID.eq(id))
                .remove();

        // 删除基本表
        // delete from role where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查角色是否存在
        // select count(*) from role where id in (?)
        if (QueryChain.of(mapper)
                .where(ROLE.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, "至少一个角色数据不存在");
        }

        // 删除中间表
        // delete from role_menu where fk_role_id in (?)
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.in(ids))
                .remove();
        // delete from user_role where fk_role_id in (?)
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_ROLE_ID.in(ids))
                .remove();

        // 删除基本表
        // delete from role where id in (?)
        if (mapper.deleteBatchByIds(ids) != ids.size()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查角色是否存在，如果不存在则直接抛出异常
     *
     * @param id 角色主键
     */
    private void existsById(Long id) {
        // select count(*) from role where id = ?
        if (!QueryChain.of(mapper)
                .where(ROLE.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, id + "号角色数据不存在");
        }
    }

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        // select fk_role_id from user_role where fk_user_id = ?
        return QueryChain.of(UserRole.class)
                .select(USER_ROLE.FK_ROLE_ID)
                .where(USER_ROLE.FK_USER_ID.eq(userId))
                .objListAs(Long.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateRolesByUserId(Long userId, List<Long> roleIds) {

        // 清空中间表：删除中间表中该用户的全部角色记录
        // delete from user_role where fk_user_id = ?
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_USER_ID.eq(userId))
                .remove();

        // 新角色列表为空，直接返回即可
        if (CollUtil.isEmpty(roleIds)) {
            return true;
        }

        // 添加中间表：在中间表中批量添加该用户的新角色记录
        List<UserRole> userRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setFkUserId(userId);
            userRole.setFkRoleId(roleId);
            userRole.setCreated(LocalDateTime.now());
            userRole.setUpdated(LocalDateTime.now());
            userRoles.add(userRole);
        }

        // insert into user_role (fk_user_id, fk_role_id, created, updated) values (?, ?, ?, ?)
        if(userRoleMapper.insertBatch(userRoles) != roleIds.size()){
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库批量更新角色记录失败");
        }
        return true;
    }




}
