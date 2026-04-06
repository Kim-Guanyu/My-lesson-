package com.mdkj.controller;

import com.mdkj.dto.SeckillDetailInsertDTO;
import com.mdkj.dto.SeckillDetailPageDTO;
import com.mdkj.dto.SeckillDetailUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.SeckillDetail;
import com.mdkj.service.SeckillDetailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 秒杀明细表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "秒杀明细表接口")
@RequestMapping("/api/v1/seckillDetail")
public class SeckillDetailController {

    @Autowired
    private SeckillDetailService seckillDetailService;

    /**
     * 添加秒杀明细表。
     *
     * @param seckillDetail 秒杀明细表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存秒杀明细表")
    public boolean save(@RequestBody @Parameter(description="秒杀明细表")SeckillDetail seckillDetail) {
        return seckillDetailService.save(seckillDetail);
    }

    /**
     * 根据主键删除秒杀明细表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键秒杀明细表")
    public boolean remove(@PathVariable @Parameter(description="秒杀明细表主键")Long id) {
        return seckillDetailService.removeById(id);
    }


    /**
     * 查询所有秒杀明细表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有秒杀明细表")
    public List<SeckillDetail> list() {
        return seckillDetailService.list();
    }

    /**
     * 根据秒杀明细表主键获取详细信息。
     *
     * @param id 秒杀明细表主键
     * @return 秒杀明细表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取秒杀明细表")
    public SeckillDetail getInfo(@PathVariable Long id) {
        return seckillDetailService.getById(id);
    }



    @Operation(summary = "新增 - 单条新增", description = "新增一条秒杀明细记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody SeckillDetailInsertDTO dto) {
        return seckillDetailService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条秒杀明细记录")
    @GetMapping("select/{id}")
    public SeckillDetail select(@PathVariable("id") Long id) {
        return seckillDetailService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询秒杀明细记录")
    @GetMapping("page")
    public PageVO<SeckillDetail> page(@Validated SeckillDetailPageDTO dto) {
        return seckillDetailService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条秒杀明细记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody SeckillDetailUpdateDTO dto) {
        return seckillDetailService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条秒杀明细记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return seckillDetailService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除秒杀明细记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return seckillDetailService.deleteBatch(ids);
    }


}
