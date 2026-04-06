package com.mdkj.controller;

import com.mdkj.dto.MenuInsertDTO;
import com.mdkj.dto.MenuPageDTO;
import com.mdkj.dto.MenuSimpleListVO;
import com.mdkj.dto.MenuUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Menu;
import com.mdkj.service.MenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 菜单表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "菜单表接口")
@RequestMapping("/api/v1/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 添加菜单表。
     *
     * @param menu 菜单表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存菜单表")
    public boolean save(@RequestBody @Parameter(description="菜单表")Menu menu) {
        return menuService.save(menu);
    }

    /**
     * 根据主键删除菜单表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键菜单表")
    public boolean remove(@PathVariable @Parameter(description="菜单表主键")Long id) {
        return menuService.removeById(id);
    }


    /**
     * 查询所有菜单表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有菜单表")
    public List<Menu> list() {
        return menuService.list();
    }

    /**
     * 根据菜单表主键获取详细信息。
     *
     * @param id 菜单表主键
     * @return 菜单表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取菜单表")
    public Menu getInfo(@PathVariable Long id) {
        return menuService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条菜单记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody MenuInsertDTO dto) {
        return menuService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条菜单记录")
    @GetMapping("select/{id}")
    public Menu select(@PathVariable("id") Long id) {
        return menuService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部菜单记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<MenuSimpleListVO> simpleList() {
        return menuService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询菜单记录")
    @GetMapping("page")
    public PageVO<Menu> page(@Validated MenuPageDTO dto) {
        return menuService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条菜单记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody MenuUpdateDTO dto) {
        return menuService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条菜单记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return menuService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除菜单记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return menuService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 角色菜单ID列表", description = "按角色主键查询角色的全部菜单ID列表")
    @GetMapping("listMenuIdsByRoleId/{roleId}")
    public List<Long> listMenuIdsByRoleId(@PathVariable("roleId") Long roleId) {
        return menuService.listMenuIdsByRoleId(roleId);
    }

    @Operation(summary = "修改 - 菜单列表", description = "按角色主键修改角色的菜单列表")
    @PutMapping("updateMenusByRoleId")
    public boolean updateMenusByRoleId(@RequestParam("roleId") Long roleId, @RequestParam("menuIds") List<Long> menuIds) {
        return menuService.updateMenusByRoleId(roleId, menuIds);
    }




}
