package com.mdkj.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.UserRole;
import com.mdkj.mapper.UserRoleMapper;
import com.mdkj.service.UserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关系表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>  implements UserRoleService{

}
