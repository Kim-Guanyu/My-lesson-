package com.mdkj.controller;

import com.mdkj.dto.BannerInsertDTO;
import com.mdkj.dto.BannerPageDTO;
import com.mdkj.dto.BannerUpdateDTO;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Banner;
import com.mdkj.service.BannerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 横幅表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "横幅表接口")
@RequestMapping("/api/v1/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 添加横幅表。
     *
     * @param banner 横幅表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存横幅表")
    public boolean save(@RequestBody @Parameter(description="横幅表")Banner banner) {
        return bannerService.save(banner);
    }

    /**
     * 根据主键删除横幅表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键横幅表")
    public boolean remove(@PathVariable @Parameter(description="横幅表主键")Long id) {
        return bannerService.removeById(id);
    }


    /**
     * 查询所有横幅表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有横幅表")
    public List<Banner> list() {
        return bannerService.list();
    }

    /**
     * 根据横幅表主键获取详细信息。
     *
     * @param id 横幅表主键
     * @return 横幅表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取横幅表")
    public Banner getInfo(@PathVariable Long id) {
        return bannerService.getById(id);
    }



    @Operation(summary = "新增 - 单条新增", description = "新增一条横幅记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody BannerInsertDTO dto) {
        return bannerService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条横幅记录")
    @GetMapping("select/{id}")
    public Banner select(@PathVariable("id") Long id) {
        return bannerService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询横幅记录")
    @GetMapping("page")
    public PageVO<Banner> page(@Validated BannerPageDTO dto) {
        return bannerService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条横幅记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody BannerUpdateDTO dto) {
        return bannerService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条横幅记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return bannerService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除横幅记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return bannerService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 前N条记录", description = "查询前N条横幅记录")
    @GetMapping("top/{n}")
    public List<Banner> top(@PathVariable("n") Long n) {
        return bannerService.top(n);
    }

    @Operation(summary = "修改 - 横幅图片", description = "按主键修改横幅图片")
    @PostMapping("/uploadBanner/{id}")
    public Result<String> uploadBanner(@RequestParam("bannerFile") MultipartFile bannerFile, @PathVariable("id") Long id) {
        return new Result<>(bannerService.uploadBanner(bannerFile, id));
    }




}
