package com.mdkj.controller;

import com.mdkj.dto.CommentInsertDTO;
import com.mdkj.dto.CommentPageDTO;
import com.mdkj.dto.CommentSimpleListVO;
import com.mdkj.dto.CommentUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Comment;
import com.mdkj.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 评论表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "评论表接口")
@RequestMapping("/api/v1/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 添加评论表。
     *
     * @param comment 评论表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存评论表")
    public boolean save(@RequestBody @Parameter(description="评论表")Comment comment) {
        return commentService.save(comment);
    }

    /**
     * 根据主键删除评论表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键评论表")
    public boolean remove(@PathVariable @Parameter(description="评论表主键")Long id) {
        return commentService.removeById(id);
    }


    /**
     * 查询所有评论表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有评论表")
    public List<Comment> list() {
        return commentService.list();
    }

    /**
     * 根据评论表主键获取详细信息。
     *
     * @param id 评论表主键
     * @return 评论表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取评论表")
    public Comment getInfo(@PathVariable Long id) {
        return commentService.getById(id);
    }





    @Operation(summary = "新增 - 单条新增", description = "新增一条评论记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody CommentInsertDTO dto) {
        return commentService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条评论记录")
    @GetMapping("select/{id}")
    public Comment select(@PathVariable("id") Long id) {
        return commentService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部评论记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<CommentSimpleListVO> simpleList() {
        return commentService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询评论记录")
    @GetMapping("page")
    public PageVO<Comment> page(@Validated CommentPageDTO dto) {
        return commentService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条评论记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody CommentUpdateDTO dto) {
        return commentService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条评论记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return commentService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除评论记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return commentService.deleteBatch(ids);
    }

    @Operation(summary = "删除 - 根据用户删除", description = "按用户主键删除评论记录")
    @DeleteMapping("deleteByUserId/{userId}")
    public boolean deleteByUserId(@PathVariable("userId") Long userId) {
        return commentService.deleteByUserId(userId);
    }

    @Operation(summary = "删除 - 根据用户删除批删", description = "按用户主键列表批量删除评论记录")
    @DeleteMapping("deleteByUserIds")
    public boolean deleteByUserIds(@RequestParam("userIds") List<Long> userIds) {
        return commentService.deleteByUserIds(userIds);
    }




}
