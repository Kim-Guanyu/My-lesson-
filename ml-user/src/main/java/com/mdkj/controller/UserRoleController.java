package com.mdkj.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.UserRole;
import com.mdkj.service.UserRoleService;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import com.mdkj.dto.PageDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;

/**
 * 用户角色关系表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "用户角色关系表接口")
@RequestMapping("/api/v1/userRole")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    /**
     * 添加用户角色关系表。
     *
     * @param userRole 用户角色关系表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存用户角色关系表")
    public boolean save(@RequestBody @Parameter(description="用户角色关系表")UserRole userRole) {
        return userRoleService.save(userRole);
    }

    /**
     * 根据主键删除用户角色关系表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键用户角色关系表")
    public boolean remove(@PathVariable @Parameter(description="用户角色关系表主键")Long id) {
        return userRoleService.removeById(id);
    }

    /**
     * 根据主键更新用户角色关系表。
     *
     * @param userRole 用户角色关系表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    @Operation(description="根据主键更新用户角色关系表")
    public boolean update(@RequestBody @Parameter(description="用户角色关系表主键")UserRole userRole) {
        return userRoleService.updateById(userRole);
    }

    /**
     * 查询所有用户角色关系表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有用户角色关系表")
    public List<UserRole> list() {
        return userRoleService.list();
    }

    /**
     * 根据用户角色关系表主键获取详细信息。
     *
     * @param id 用户角色关系表主键
     * @return 用户角色关系表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取用户角色关系表")
    public UserRole getInfo(@PathVariable Long id) {
        return userRoleService.getById(id);
    }

    /**
     * 分页查询用户角色关系表。
     *
     * @param dto 分页参数
     * @return 分页对象
     */
    @GetMapping("page")
    @Operation(description="分页查询用户角色关系表")
    public PageVO<UserRole> page(@Validated PageDTO dto) {
        Page<UserRole> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<UserRole> result = userRoleService.page(page);
        PageVO<UserRole> pageVO = new PageVO<>();
        pageVO.setPageNum(result.getPageNumber());
        pageVO.setPageSize((int) result.getPageSize());
        pageVO.setTotalRow(result.getTotalRow());
        pageVO.setTotalPage((int) result.getTotalPage());
        pageVO.setRecords(result.getRecords());
        return pageVO;
    }

}
