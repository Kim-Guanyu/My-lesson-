package com.mdkj.service;

import com.mdkj.dto.MenuInsertDTO;
import com.mdkj.dto.MenuPageDTO;
import com.mdkj.dto.MenuSimpleListVO;
import com.mdkj.dto.MenuUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Menu;

import java.util.List;

/**
 * 菜单表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface MenuService extends IService<Menu> {
    boolean insert(MenuInsertDTO dto);
    Menu select(Long id);
    List<MenuSimpleListVO> simpleList();
    PageVO<Menu> page(MenuPageDTO dto);
    boolean update(MenuUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    /**
     * 按角色主键查询该角色的全部菜单ID列表
     *
     * @param roleId 角色主键
     * @return 该角色的全部菜单ID列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);
    /**
     * 按角色主键修改该角色的菜单列表
     *
     * @param roleId  角色主键
     * @param menuIds 菜单主键列表
     */
    boolean updateMenusByRoleId(Long roleId, List<Long> menuIds);




}
