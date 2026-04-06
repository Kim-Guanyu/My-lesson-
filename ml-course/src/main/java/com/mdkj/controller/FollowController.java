package com.mdkj.controller;

import com.mdkj.dto.FollowInsertDTO;
import com.mdkj.dto.FollowUpdateDTO;
import com.mybatisflex.core.paginate.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Follow;
import com.mdkj.service.FollowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 收藏表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "收藏表接口")
@RequestMapping("/api/v1/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 添加收藏表。
     *
     * @param follow 收藏表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存收藏表")
    public boolean save(@RequestBody @Parameter(description="收藏表")Follow follow) {
        return followService.save(follow);
    }

    /**
     * 根据主键删除收藏表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键收藏表")
    public boolean remove(@PathVariable @Parameter(description="收藏表主键")Long id) {
        return followService.removeById(id);
    }


    /**
     * 查询所有收藏表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有收藏表")
    public List<Follow> list() {
        return followService.list();
    }

    /**
     * 根据收藏表主键获取详细信息。
     *
     * @param id 收藏表主键
     * @return 收藏表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取收藏表")
    public Follow getInfo(@PathVariable Long id) {
        return followService.getById(id);
    }

    /**
     * 分页查询收藏表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    @Operation(description="分页查询收藏表")
    public Page<Follow> page(@Parameter(description="分页信息")Page<Follow> page) {
        return followService.page(page);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条收藏记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody FollowInsertDTO dto) {
        return followService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条收藏记录")
    @GetMapping("select/{id}")
    public Follow select(@PathVariable("id") Long id) {
        return followService.select(id);
    }


    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条收藏记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody FollowUpdateDTO dto) {
        return followService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条收藏记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return followService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除收藏记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return followService.deleteBatch(ids);
    }


}
