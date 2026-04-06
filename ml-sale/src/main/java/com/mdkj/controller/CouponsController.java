package com.mdkj.controller;

import com.mdkj.dto.CouponsInsertDTO;
import com.mdkj.dto.CouponsPageDTO;
import com.mdkj.dto.CouponsSimpleListVO;
import com.mdkj.dto.CouponsUpdateDTO;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Coupons;
import com.mdkj.service.CouponsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 优惠卷表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "优惠卷表接口")
@RequestMapping("/api/v1/coupons")
public class CouponsController {

    @Autowired
    private CouponsService couponsService;

    /**
     * 添加优惠卷表。
     *
     * @param coupons 优惠卷表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存优惠卷表")
    public boolean save(@RequestBody @Parameter(description="优惠卷表")Coupons coupons) {
        return couponsService.save(coupons);
    }

    /**
     * 根据主键删除优惠卷表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键优惠卷表")
    public boolean remove(@PathVariable @Parameter(description="优惠卷表主键")Long id) {
        return couponsService.removeById(id);
    }


    /**
     * 查询所有优惠卷表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有优惠卷表")
    public List<Coupons> list() {
        return couponsService.list();
    }

    /**
     * 根据优惠卷表主键获取详细信息。
     *
     * @param id 优惠卷表主键
     * @return 优惠卷表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取优惠卷表")
    public Coupons getInfo(@PathVariable Long id) {
        return couponsService.getById(id);
    }



    @Operation(summary = "新增 - 单条新增", description = "新增一条优惠卷记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody CouponsInsertDTO dto) {
        return couponsService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条优惠卷记录")
    @GetMapping("select/{id}")
    public Coupons select(@PathVariable("id") Long id) {
        return couponsService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部优惠卷记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<CouponsSimpleListVO> simpleList() {
        return couponsService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询优惠卷记录")
    @GetMapping("page")
    public PageVO<Coupons> page(@Validated CouponsPageDTO dto) {
        return couponsService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条优惠卷记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody CouponsUpdateDTO dto) {
        return couponsService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条优惠卷记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return couponsService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除优惠卷记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return couponsService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 兑换口令", description = "按兑换口令查询一条优惠卷记录")
    @GetMapping("selectByCode/{code}")
    public Coupons selectByCode(@PathVariable("code") String code) {
        return couponsService.selectByCode(code);
    }



}
