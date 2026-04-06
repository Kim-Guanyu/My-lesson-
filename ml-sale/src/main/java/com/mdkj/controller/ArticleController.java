package com.mdkj.controller;

import com.mdkj.dto.ArticleInsertDTO;
import com.mdkj.dto.ArticlePageDTO;
import com.mdkj.dto.ArticleUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Article;
import com.mdkj.service.ArticleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 新闻表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "新闻表接口")
@RequestMapping("/api/v1/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * 添加新闻表。
     *
     * @param article 新闻表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存新闻表")
    public boolean save(@RequestBody @Parameter(description="新闻表")Article article) {
        return articleService.save(article);
    }

    /**
     * 根据主键删除新闻表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键新闻表")
    public boolean remove(@PathVariable @Parameter(description="新闻表主键")Long id) {
        return articleService.removeById(id);
    }

    /**
     * 根据主键更新新闻表。
     *
     * @param article 新闻表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    @Operation(description="根据主键更新新闻表")
    public boolean update(@RequestBody @Parameter(description="新闻表主键")Article article) {
        return articleService.updateById(article);
    }

    /**
     * 查询所有新闻表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有新闻表")
    public List<Article> list() {
        return articleService.list();
    }

    /**
     * 根据新闻表主键获取详细信息。
     *
     * @param id 新闻表主键
     * @return 新闻表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取新闻表")
    public Article getInfo(@PathVariable Long id) {
        return articleService.getById(id);
    }



    @Operation(summary = "新增 - 单条新增", description = "新增一条新闻记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody ArticleInsertDTO dto) {
        return articleService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条新闻记录")
    @GetMapping("select/{id}")
    public Article select(@PathVariable("id") Long id) {
        return articleService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询新闻记录")
    @GetMapping("page")
    public PageVO<Article> page(@Validated ArticlePageDTO dto) {
        return articleService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条新闻记录")
    @PutMapping("update/{id}")
    public boolean update(@Validated @RequestBody ArticleUpdateDTO dto) {
        return articleService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条新闻记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return articleService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除新闻记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return articleService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 前N条记录", description = "查询前N条新闻记录")
    @GetMapping("top/{n}")
    public List<Article> top(@PathVariable("n") Long n) {
        return articleService.top(n);
    }



}
