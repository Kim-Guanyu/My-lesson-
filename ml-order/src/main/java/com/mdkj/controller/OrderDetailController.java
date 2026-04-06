package com.mdkj.controller;

import com.mdkj.dto.OrderDetailInsertDTO;
import com.mdkj.dto.OrderDetailPageDTO;
import com.mdkj.dto.OrderDetailUpdateDTO;
import com.mdkj.util.EasyExcelUtil;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.OrderDetail;
import com.mdkj.service.OrderDetailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 订单明细表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "订单明细表接口")
@RequestMapping("/api/v1/orderDetail")
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 添加订单明细表。
     *
     * @param orderDetail 订单明细表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存订单明细表")
    public boolean save(@RequestBody @Parameter(description="订单明细表")OrderDetail orderDetail) {
        return orderDetailService.save(orderDetail);
    }

    /**
     * 根据主键删除订单明细表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键订单明细表")
    public boolean remove(@PathVariable @Parameter(description="订单明细表主键")Long id) {
        return orderDetailService.removeById(id);
    }


    /**
     * 查询所有订单明细表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有订单明细表")
    public List<OrderDetail> list() {
        return orderDetailService.list();
    }

    /**
     * 根据订单明细表主键获取详细信息。
     *
     * @param id 订单明细表主键
     * @return 订单明细表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取订单明细表")
    public OrderDetail getInfo(@PathVariable Long id) {
        return orderDetailService.getById(id);
    }

    @Operation(summary = "新增 - 单条新增", description = "新增一条订单明细记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody OrderDetailInsertDTO dto) {
        return orderDetailService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条订单明细记录")
    @GetMapping("select/{id}")
    public OrderDetail select(@PathVariable("id") Long id) {
        return orderDetailService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询订单明细记录")
    @GetMapping("page")
    public PageVO<OrderDetail> page(@Validated OrderDetailPageDTO dto) {
        return orderDetailService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条订单明细记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody OrderDetailUpdateDTO dto) {
        return orderDetailService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条订单明细记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return orderDetailService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除订单明细记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return orderDetailService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 报表打印", description = "打印订单明细相关的报表数据")
    @GetMapping("excel")
    public void excel(HttpServletResponse response) {
        EasyExcelUtil.download(response, "订单明细统计表", orderDetailService.getExcelData());
    }



}
