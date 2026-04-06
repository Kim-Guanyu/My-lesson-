package com.mdkj.service;

import com.mdkj.dto.RoleInsertDTO;
import com.mdkj.dto.RolePageDTO;
import com.mdkj.dto.RoleSimpleListVO;
import com.mdkj.dto.RoleUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Role;

import java.util.List;

/**
 * 角色表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface RoleService extends IService<Role> {
    boolean insert(RoleInsertDTO dto);
    Role select(Long id);
    List<RoleSimpleListVO> simpleList();
    PageVO<Role> page(RolePageDTO dto);
    boolean update(RoleUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    /**
     * 按用户主键查询该用户的全部角色ID列表
     *
     * @param userId 用户主键
     * @return 该用户的全部角色ID列表
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 按用户主键修改该用户的角色列表
     *
     * @param userId  用户主键
     * @param roleIds 角色主键列表
     */
    boolean updateRolesByUserId(Long userId, List<Long> roleIds);



}
