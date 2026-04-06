package com.mdkj.controller;

import com.mdkj.dto.RoleInsertDTO;
import com.mdkj.dto.RolePageDTO;
import com.mdkj.dto.RoleSimpleListVO;
import com.mdkj.dto.RoleUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Role;
import com.mdkj.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 角色表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "角色表接口")
@RequestMapping("/api/v1/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 添加角色表。
     *
     * @param role 角色表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存角色表")
    public boolean save(@RequestBody @Parameter(description="角色表")Role role) {
        return roleService.save(role);
    }

    /**
     * 根据主键删除角色表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键角色表")
    public boolean remove(@PathVariable @Parameter(description="角色表主键")Long id) {
        return roleService.removeById(id);
    }

    /**
     * 查询所有角色表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有角色表")
    public List<Role> list() {
        return roleService.list();
    }

    /**
     * 根据角色表主键获取详细信息。
     *
     * @param id 角色表主键
     * @return 角色表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取角色表")
    public Role getInfo(@PathVariable Long id) {
        return roleService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条角色记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody RoleInsertDTO dto) {
        return roleService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条角色记录")
    @GetMapping("select/{id}")
    public Role select(@PathVariable("id") Long id) {
        return roleService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部角色记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<RoleSimpleListVO> simpleList() {
        return roleService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询角色记录")
    @GetMapping("page")
    public PageVO<Role> page(@Validated RolePageDTO dto) {
        return roleService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条角色记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody RoleUpdateDTO dto) {
        return roleService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条角色记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return roleService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除角色记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return roleService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 用户角色ID列表", description = "按用户主键查询用户的全部角色ID列表")
    @GetMapping("listRoleIdsByUserId/{userId}")
    public List<Long> listRoleIdsByUserId(@PathVariable("userId") Long userId) {
        return roleService.listRoleIdsByUserId(userId);
    }

    @Operation(summary = "修改 - 角色列表", description = "按用户主键修改用户的角色列表")
    @PutMapping("updateRolesByUserId")
    public boolean updateRolesByUserId(@RequestParam("userId") Long userId, @RequestParam("roleIds") List<Long> roleIds) {
        return roleService.updateRolesByUserId(userId, roleIds);
    }




}
