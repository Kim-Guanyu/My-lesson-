package com.mdkj.controller;

import com.mdkj.dto.CourseInsertDTO;
import com.mdkj.dto.CoursePageDTO;
import com.mdkj.dto.CourseSimpleListVO;
import com.mdkj.dto.CourseUpdateDTO;
import com.mdkj.es.CourseDoc;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Course;
import com.mdkj.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "课程表接口")
@RequestMapping("/api/v1/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 添加课程表。
     *
     * @param course 课程表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存课程表")
    public boolean save(@RequestBody @Parameter(description="课程表")Course course) {
        return courseService.save(course);
    }

    /**
     * 根据主键删除课程表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键课程表")
    public boolean remove(@PathVariable @Parameter(description="课程表主键")Long id) {
        return courseService.removeById(id);
    }


    /**
     * 查询所有课程表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有课程表")
    public List<Course> list() {
        return courseService.list();
    }

    /**
     * 根据课程表主键获取详细信息。
     *
     * @param id 课程表主键
     * @return 课程表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取课程表")
    public Course getInfo(@PathVariable Long id) {
        return courseService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条课程记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody CourseInsertDTO dto) {
        return courseService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条课程记录")
    @GetMapping("select/{id}")
    public Course select(@PathVariable("id") Long id) {
        return courseService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部课程记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<CourseSimpleListVO> simpleList() {
        return courseService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询课程记录")
    @GetMapping("page")
    public PageVO<Course> page(@Validated CoursePageDTO dto) {
        return courseService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条课程记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody CourseUpdateDTO dto) {
        return courseService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条课程记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return courseService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除课程记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return courseService.deleteBatch(ids);
    }

    @Operation(summary = "修改 - 课程封面", description = "按主键修改课程的封面图片")
    @PostMapping("/uploadCover/{id}")
    public Result<String> uploadCover(@RequestParam("coverFile") MultipartFile coverFile, @PathVariable("id") Long id) {
        return new Result<>(courseService.uploadCover(coverFile, id));
    }

    @Operation(summary = "修改 - 课程摘要", description = "按主键修改课程的摘要图片")
    @PostMapping("/uploadSummary/{id}")
    public Result<String> uploadSummary(@RequestParam("summaryFile") MultipartFile summaryFile, @PathVariable("id") Long id) {
        return new Result<>(courseService.uploadSummary(summaryFile, id));
    }

    @Operation(summary = "搜索 - 课程列表", description = "按课程名称或作者名称分页搜索课程信息")
    @GetMapping("search")
    public PageVO<CourseDoc> search(@Validated CoursePageDTO dto) {
        return courseService.search(dto);
    }





}
