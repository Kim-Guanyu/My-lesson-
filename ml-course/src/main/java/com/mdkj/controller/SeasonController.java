package com.mdkj.controller;

import com.mdkj.dto.SeasonInsertDTO;
import com.mdkj.dto.SeasonPageDTO;
import com.mdkj.dto.SeasonSimpleListVO;
import com.mdkj.dto.SeasonUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Season;
import com.mdkj.service.SeasonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 季次表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "季次表接口")
@RequestMapping("/api/v1/season")
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    /**
     * 添加季次表。
     *
     * @param season 季次表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存季次表")
    public boolean save(@RequestBody @Parameter(description="季次表")Season season) {
        return seasonService.save(season);
    }

    /**
     * 根据主键删除季次表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键季次表")
    public boolean remove(@PathVariable @Parameter(description="季次表主键")Long id) {
        return seasonService.removeById(id);
    }


    /**
     * 查询所有季次表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有季次表")
    public List<Season> list() {
        return seasonService.list();
    }

    /**
     * 根据季次表主键获取详细信息。
     *
     * @param id 季次表主键
     * @return 季次表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取季次表")
    public Season getInfo(@PathVariable Long id) {
        return seasonService.getById(id);
    }




    @Operation(summary = "新增 - 单条新增", description = "新增一条季次记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody SeasonInsertDTO dto) {
        return seasonService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条季次记录")
    @GetMapping("select/{id}")
    public Season select(@PathVariable("id") Long id) {
        return seasonService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部季次记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<SeasonSimpleListVO> simpleList() {
        return seasonService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询季次记录")
    @GetMapping("page")
    public PageVO<Season> page(@Validated SeasonPageDTO dto) {
        return seasonService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条季次记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody SeasonUpdateDTO dto) {
        return seasonService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条季次记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return seasonService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除季次记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return seasonService.deleteBatch(ids);
    }


}
