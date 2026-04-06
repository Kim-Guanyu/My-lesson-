package com.mdkj.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.RoleMenu;
import com.mdkj.mapper.RoleMenuMapper;
import com.mdkj.service.RoleMenuService;
import org.springframework.stereotype.Service;

/**
 * 角色菜单关系表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu>  implements RoleMenuService{

}
