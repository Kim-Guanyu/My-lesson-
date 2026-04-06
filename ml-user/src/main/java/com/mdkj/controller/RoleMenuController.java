package com.mdkj.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.RoleMenu;
import com.mdkj.service.RoleMenuService;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import com.mdkj.dto.PageDTO;
import com.mdkj.vo.PageVO;

/**
 * 角色菜单关系表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "角色菜单关系表接口")
@RequestMapping("/api/v1/roleMenu")
public class RoleMenuController {

    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 添加角色菜单关系表。
     *
     * @param roleMenu 角色菜单关系表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存角色菜单关系表")
    public boolean save(@RequestBody @Parameter(description="角色菜单关系表")RoleMenu roleMenu) {
        return roleMenuService.save(roleMenu);
    }

    /**
     * 根据主键删除角色菜单关系表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键角色菜单关系表")
    public boolean remove(@PathVariable @Parameter(description="角色菜单关系表主键")Long id) {
        return roleMenuService.removeById(id);
    }

    /**
     * 根据主键更新角色菜单关系表。
     *
     * @param roleMenu 角色菜单关系表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    @Operation(description="根据主键更新角色菜单关系表")
    public boolean update(@RequestBody @Parameter(description="角色菜单关系表主键")RoleMenu roleMenu) {
        return roleMenuService.updateById(roleMenu);
    }

    /**
     * 查询所有角色菜单关系表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有角色菜单关系表")
    public List<RoleMenu> list() {
        return roleMenuService.list();
    }

    /**
     * 根据角色菜单关系表主键获取详细信息。
     *
     * @param id 角色菜单关系表主键
     * @return 角色菜单关系表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取角色菜单关系表")
    public RoleMenu getInfo(@PathVariable Long id) {
        return roleMenuService.getById(id);
    }

    /**
     * 分页查询角色菜单关系表。
     *
     * @param dto 分页参数
     * @return 分页对象
     */
    @GetMapping("page")
    @Operation(description="分页查询角色菜单关系表")
    public PageVO<RoleMenu> page(@Validated PageDTO dto) {
        Page<RoleMenu> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<RoleMenu> result = roleMenuService.page(page);
        PageVO<RoleMenu> pageVO = new PageVO<>();
        pageVO.setPageNum(result.getPageNumber());
        pageVO.setPageSize((int) result.getPageSize());
        pageVO.setTotalRow(result.getTotalRow());
        pageVO.setTotalPage((int) result.getTotalPage());
        pageVO.setRecords(result.getRecords());
        return pageVO;
    }

}
