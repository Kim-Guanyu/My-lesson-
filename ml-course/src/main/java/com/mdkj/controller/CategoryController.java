package com.mdkj.controller;

import com.mdkj.dto.CategoryInsertDTO;
import com.mdkj.dto.CategoryPageDTO;
import com.mdkj.dto.CategorySimpleListVO;
import com.mdkj.dto.CategoryUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Category;
import com.mdkj.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 课程类别表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "课程类别表接口")
@RequestMapping("/api/v1/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 添加课程类别表。
     *
     * @param category 课程类别表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存课程类别表")
    public boolean save(@RequestBody @Parameter(description="课程类别表")Category category) {
        return categoryService.save(category);
    }

    /**
     * 根据主键删除课程类别表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键课程类别表")
    public boolean remove(@PathVariable @Parameter(description="课程类别表主键")Long id) {
        return categoryService.removeById(id);
    }


    /**
     * 查询所有课程类别表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有课程类别表")
    public List<Category> list() {
        return categoryService.list();
    }

    /**
     * 根据课程类别表主键获取详细信息。
     *
     * @param id 课程类别表主键
     * @return 课程类别表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取课程类别表")
    public Category getInfo(@PathVariable Long id) {
        return categoryService.getById(id);
    }




    @Operation(summary = "新增 - 单条新增", description = "新增一条类别记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody CategoryInsertDTO dto) {
        return categoryService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条类别记录")
    @GetMapping("select/{id}")
    public Category select(@PathVariable("id") Long id) {
        return categoryService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部类别记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<CategorySimpleListVO> simpleList() {
        return categoryService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询类别记录")
    @GetMapping("page")
    public PageVO<Category> page(@Validated CategoryPageDTO dto) {
        return categoryService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条类别记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody CategoryUpdateDTO dto) {
        return categoryService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条类别记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return categoryService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除类别记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return categoryService.deleteBatch(ids);
    }


}
