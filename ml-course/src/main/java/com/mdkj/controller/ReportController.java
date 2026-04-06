package com.mdkj.controller;

import com.mdkj.dto.ReportInsertDTO;
import com.mdkj.dto.ReportPageDTO;
import com.mdkj.dto.ReportUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Report;
import com.mdkj.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 举报表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "举报表接口")
@RequestMapping("/api/v1/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 添加举报表。
     *
     * @param report 举报表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存举报表")
    public boolean save(@RequestBody @Parameter(description="举报表")Report report) {
        return reportService.save(report);
    }

    /**
     * 根据主键删除举报表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键举报表")
    public boolean remove(@PathVariable @Parameter(description="举报表主键")Long id) {
        return reportService.removeById(id);
    }


    /**
     * 查询所有举报表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有举报表")
    public List<Report> list() {
        return reportService.list();
    }

    /**
     * 根据举报表主键获取详细信息。
     *
     * @param id 举报表主键
     * @return 举报表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取举报表")
    public Report getInfo(@PathVariable Long id) {
        return reportService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条举报记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody ReportInsertDTO dto) {
        return reportService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条举报记录")
    @GetMapping("select/{id}")
    public Report select(@PathVariable("id") Long id) {
        return reportService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询举报记录")
    @GetMapping("page")
    public PageVO<Report> page(@Validated ReportPageDTO dto) {
        return reportService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条举报记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody ReportUpdateDTO dto) {
        return reportService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条举报记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return reportService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除举报记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return reportService.deleteBatch(ids);
    }

    @Operation(summary = "删除 - 根据用户删除", description = "按用户主键删除举报记录")
    @DeleteMapping("deleteByUserId/{userId}")
    public boolean deleteByUserId(@PathVariable("userId") Long userId) {
        return reportService.deleteByUserId(userId);
    }

    @Operation(summary = "删除 - 根据用户批删", description = "按用户主键列表批量删除举报记录")
    @DeleteMapping("deleteByUserIds")
    public boolean deleteByUserIds(@RequestParam("userIds") List<Long> userIds) {
        return reportService.deleteByUserIds(userIds);
    }




}
