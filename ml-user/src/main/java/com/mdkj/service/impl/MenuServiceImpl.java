package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.MenuInsertDTO;
import com.mdkj.dto.MenuPageDTO;
import com.mdkj.dto.MenuSimpleListVO;
import com.mdkj.dto.MenuUpdateDTO;
import com.mdkj.entity.RoleMenu;
import com.mdkj.exception.ServiceException;
import com.mdkj.mapper.RoleMenuMapper;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Menu;
import com.mdkj.mapper.MenuMapper;
import com.mdkj.service.MenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mdkj.entity.table.MenuTableDef.MENU;
import static com.mdkj.entity.table.RoleMenuTableDef.ROLE_MENU;

/**
 * 菜单表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu>  implements MenuService{

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Override
    public boolean insert(MenuInsertDTO dto) {
        String title = dto.getTitle();

        // 标题查重
        // select count(*) from menu where title = ?
        if (QueryChain.of(mapper)
                .where(MENU.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Menu menu = BeanUtil.copyProperties(dto, Menu.class);
        menu.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        menu.setCreated(LocalDateTime.now());
        menu.setUpdated(LocalDateTime.now());

        // insert into menu (title, info, url, icon, pid, idx, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(menu) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据添加失败");
        }
        return true;
    }

    @Override
    public Menu select(Long id) {
        // select * from menu where id = ?
        Menu menu = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(menu)) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, id + "号数据不存在");
        }
        return menu;
    }

    @Override
    public List<MenuSimpleListVO> simpleList() {

        // 查询全部菜单记录（联查父菜单记录）
        // select * from menu order by pid asc, idx asc, id desc
        List<Menu> menus = QueryChain.of(mapper)
                .orderBy(MENU.PID.asc(), MENU.IDX.asc(), MENU.ID.desc())
                .withRelations()
                .list();

        // 组装 VO 实体类
        List<MenuSimpleListVO> result = new ArrayList<>();
        menus.forEach(menu -> {
            MenuSimpleListVO menuSimpleListVO = BeanUtil.copyProperties(menu, MenuSimpleListVO.class);
            if (menu.getParentMenu() != null) {
                menuSimpleListVO.setParentTitle(menu.getParentMenu().getTitle());
            }
            result.add(menuSimpleListVO);
        });
        return result;
    }

    @Override
    public PageVO<Menu> page(MenuPageDTO dto) {
        QueryChain<Menu> queryChain = QueryChain.of(mapper)
                .orderBy(MENU.PID.asc(), MENU.IDX.asc(), MENU.ID.desc());

        // pid条件
        if (ObjectUtil.isNotNull(dto.getPid())) {
            queryChain.where(MENU.PID.eq(dto.getPid()));
        }

        // title条件
        if (ObjectUtil.isNotNull(dto.getTitle())) {
            queryChain.where(MENU.TITLE.like(dto.getTitle()));
        }

        // DB分页并转为VO
        Page<Menu> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Menu> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(MenuUpdateDTO dto) {
        String title = dto.getTitle();
        Long id = dto.getId();

        // 检查菜单是否存在
        this.existsById(id);

        // 标题查重
        // select count(*) from menu where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(MENU.TITLE.eq(title))
                .and(MENU.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Menu menu = BeanUtil.copyProperties(dto, Menu.class);
        menu.setUpdated(LocalDateTime.now());

        // update menu set title = ?, info = ?, url = ?, icon = ?, pid = ?, idx = ?, updated = ? where id = ?
        if (!UpdateChain.of(menu)
                .where(MENU.ID.eq(menu.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id) {

        // 检查菜单是否存在
        this.existsById(id);

        // 查询父菜单ID和全部子菜单ID
        // select id from menu where pid = 1 or id = 1
        List<Long> deleteIds = QueryChain.of(mapper)
                .select(MENU.ID)
                .where(MENU.PID.eq(id))
                .or(MENU.ID.eq(id))
                .objListAs(Long.class);

        // 删除中间表
        // delete from role_menu where fk_menu_id in(?)
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_MENU_ID.in(deleteIds))
                .remove();

        // 删除 MENU 表中的菜单
        // delete from menu where id in(?)
        if (!UpdateChain.of(mapper)
                .where(MENU.ID.in(deleteIds))
                .remove()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查菜单是否存在
        // select count(*) from menu where id in (?)
        if (QueryChain.of(mapper)
                .where(MENU.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, "至少一个菜单数据不存在");
        }

        // 查询父菜单ID和全部子菜单ID
        // select id from menu where pid in (?) or id in (?)
        List<Long> deleteIds = QueryChain.of(mapper)
                .select(MENU.ID)
                .where(MENU.PID.in(ids))
                .or(MENU.ID.in(ids))
                .objListAs(Long.class);

        // 删除中间表
        // delete from role_menu where fk_menu_id in(?)
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_MENU_ID.in(deleteIds))
                .remove();

        // 删除 MENU 表中的菜单
        // delete from menu where id in(?)
        if (!UpdateChain.of(mapper)
                .where(MENU.ID.in(deleteIds))
                .remove()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查菜单是否存在，如果不存在则直接抛出异常
     *
     * @param id 菜单主键
     */
    private void existsById(Long id) {
        // select count(*) from menu where id = ?
        if (!QueryChain.of(mapper)
                .where(MENU.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, id + "号菜单数据不存在");
        }
    }

    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        // select fk_menu_id from role_menu where fk_role_id = xx
        return QueryChain.of(roleMenuMapper)
                .select(ROLE_MENU.FK_MENU_ID)
                .where(ROLE_MENU.FK_ROLE_ID.eq(roleId))
                .objListAs(Long.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateMenusByRoleId(Long roleId, List<Long> menuIds) {

        // 清空中间表：删除中间表中该角色的全部菜单记录
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.eq(roleId))
                .remove();

        // 新菜单列表为空，直接返回即可
        if (CollUtil.isEmpty(menuIds)) {
            return true;
        }

        // 添加中间表：在中间表中批量添加该角色的新菜单记录
        List<RoleMenu> roleMenus = new ArrayList<>();
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setFkRoleId(roleId);
            roleMenu.setFkMenuId(menuId);
            roleMenu.setCreated(LocalDateTime.now());
            roleMenu.setUpdated(LocalDateTime.now());
            roleMenus.add(roleMenu);
        }

        // insert into role_menu(fk_role_id, fk_menu_id, created, updated) values(?, ?, ?, ?)
        if (roleMenuMapper.insertBatch(roleMenus) != menuIds.size()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库批量更新菜单记录失败");
        }
        return true;
    }




}
