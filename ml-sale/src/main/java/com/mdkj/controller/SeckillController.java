package com.mdkj.controller;

import com.mdkj.dto.*;
import com.mdkj.vo.PageVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Seckill;
import com.mdkj.service.SeckillService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 秒杀表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "秒杀表接口")
@RequestMapping("/api/v1/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 添加秒杀表。
     *
     * @param seckill 秒杀表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存秒杀表")
    public boolean save(@RequestBody @Parameter(description="秒杀表")Seckill seckill) {
        return seckillService.save(seckill);
    }

    /**
     * 根据主键删除秒杀表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键秒杀表")
    public boolean remove(@PathVariable @Parameter(description="秒杀表主键")Long id) {
        return seckillService.removeById(id);
    }


    /**
     * 查询所有秒杀表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有秒杀表")
    public List<Seckill> list() {
        return seckillService.list();
    }

    /**
     * 根据秒杀表主键获取详细信息。
     *
     * @param id 秒杀表主键
     * @return 秒杀表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取秒杀表")
    public Seckill getInfo(@PathVariable Long id) {
        return seckillService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条秒杀记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody SeckillInsertDTO dto) {
        return seckillService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条秒杀记录")
    @GetMapping("select/{id}")
    public Seckill select(@PathVariable("id") Long id) {
        return seckillService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部秒杀记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<SeckillSimpleListVO> simpleList() {
        return seckillService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询秒杀记录")
    @GetMapping("page")
    public PageVO<Seckill> page(@Validated SeckillPageDTO dto) {
        return seckillService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条秒杀记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody SeckillUpdateDTO dto) {
        return seckillService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条秒杀记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return seckillService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除秒杀记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return seckillService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 今日秒杀活动", description = "查询今日的秒杀活动记录")
    @GetMapping("today")
    public List<Seckill> today() {
        return seckillService.today();
    }

    @Operation(summary = "开始秒杀", description = "秒杀购买指定的课程")
    @PostMapping("kill")
    public boolean kill(@Validated @RequestBody KillDTO dto) {
        return seckillService.kill(dto);
    }




}
