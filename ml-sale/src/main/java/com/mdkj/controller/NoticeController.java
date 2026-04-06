package com.mdkj.controller;

import com.mdkj.dto.NoticeInsertDTO;
import com.mdkj.dto.NoticePageDTO;
import com.mdkj.dto.NoticeUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Notice;
import com.mdkj.service.NoticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 通知表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "通知表接口")
@RequestMapping("/api/v1/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 添加通知表。
     *
     * @param notice 通知表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存通知表")
    public boolean save(@RequestBody @Parameter(description="通知表")Notice notice) {
        return noticeService.save(notice);
    }

    /**
     * 根据主键删除通知表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键通知表")
    public boolean remove(@PathVariable @Parameter(description="通知表主键")Long id) {
        return noticeService.removeById(id);
    }


    /**
     * 查询所有通知表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有通知表")
    public List<Notice> list() {
        return noticeService.list();
    }

    /**
     * 根据通知表主键获取详细信息。
     *
     * @param id 通知表主键
     * @return 通知表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取通知表")
    public Notice getInfo(@PathVariable Long id) {
        return noticeService.getById(id);
    }



    @Operation(summary = "新增 - 单条新增", description = "新增一条通知记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody NoticeInsertDTO dto) {
        return noticeService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条通知记录")
    @GetMapping("select/{id}")
    public Notice select(@PathVariable("id") Long id) {
        return noticeService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询通知记录")
    @GetMapping("page")
    public PageVO<Notice> page(@Validated NoticePageDTO dto) {
        return noticeService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条通知记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody NoticeUpdateDTO dto) {
        return noticeService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条通知记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return noticeService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除通知记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return noticeService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 前N条记录", description = "查询前N条通知记录")
    @GetMapping("top/{n}")
    public List<Notice> top(@PathVariable("n") Long n) {
        return noticeService.top(n);
    }



}
