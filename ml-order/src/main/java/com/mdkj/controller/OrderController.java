package com.mdkj.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.mdkj.dto.*;
import com.mdkj.util.AlipayUtil;
import com.mdkj.util.ML;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.Order;
import com.mdkj.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * 订单表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "订单表接口")
@RequestMapping("/api/v1/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 添加订单表。
     *
     * @param order 订单表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存订单表")
    public boolean save(@RequestBody @Parameter(description="订单表")Order order) {
        return orderService.save(order);
    }

    /**
     * 根据主键删除订单表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键订单表")
    public boolean remove(@PathVariable @Parameter(description="订单表主键")Long id) {
        return orderService.removeById(id);
    }



    /**
     * 查询所有订单表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有订单表")
    public List<Order> list() {
        return orderService.list();
    }

    /**
     * 根据订单表主键获取详细信息。
     *
     * @param id 订单表主键
     * @return 订单表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取订单表")
    public Order getInfo(@PathVariable Long id) {
        return orderService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条订单记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody OrderInsertDTO dto) {
        return orderService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条订单记录")
    @GetMapping("select/{id}")
    public Order select(@PathVariable("id") Long id) {
        return orderService.select(id);
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询订单记录")
    @GetMapping("page")
    public PageVO<Order> page(@Validated OrderPageDTO dto) {
        return orderService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条订单记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody OrderUpdateDTO dto) {
        return orderService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条订单记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return orderService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除订单记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return orderService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 统计数据", description = "查询订单相关的统计数据")
    @GetMapping("statistics")
    public Map<String, Object> statistics() {
        return orderService.statistics();
    }

    @Operation(summary = "添加 - 预支付", description = "用户下订单，创建一个未支付的订单")
    @PostMapping("/prePay")
    public Object prePay(@RequestBody PrePayDTO dto) {
        return new Result<>(orderService.prePay(dto));
    }

    @SneakyThrows
    @Operation(summary = "查询 - 预支付二维码", description = "获取预支付二维码")
    @PostMapping("/getQrCode")
    public void getQrCode(HttpServletResponse resp, @RequestBody QrCodeDTO qrCodeDTO) {
        // 初始化配置
        Factory.setOptions(AlipayUtil.getConfig());
        // 发起预支付请求
        AlipayTradePrecreateResponse alipayTradePrecreateResponse = Factory.Payment
                .FaceToFace()
                .preCreate("ML订单支付", qrCodeDTO.getSn(), qrCodeDTO.getPayAmount().toString());
        // 解析预支付响应
        JSONObject response = JSONUtil.parseObj(alipayTradePrecreateResponse.getHttpBody()).getJSONObject("alipay_trade_precreate_response");
        // 设置响应头：响应类型为图片，不缓存（addHeader 项是为了兼容老版本浏览器）
        resp.setContentType(MediaType.IMAGE_JPEG_VALUE);
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.addHeader("Cache-Control", "post-check=0, pre-check=0");
        // 生成二维码图片
        BufferedImage bufferedImage = QrCodeUtil.generate(response.get("qr_code").toString(), new QrConfig(300, 200));
        // 将图片写入响应输出流
        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            ImageIO.write(bufferedImage, "jpg", outputStream);
            outputStream.flush();
        }
    }

    @Operation(summary = "回调 - 预支付回调", description = "支付成功后，支付宝自动回调的接口")
    @PostMapping("/prePayNotify")
    public boolean prePayNotify(HttpServletRequest request) {
        String sn = request.getParameter("out_trade_no");
        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("交易主题: " + request.getParameter("subject"));
            System.out.println("交易状态: " + request.getParameter("trade_status"));
            System.out.println("交易凭证: " + request.getParameter("trade_no"));
            System.out.println("订单编号: " + request.getParameter("out_trade_no"));
            System.out.println("交易金额: " + request.getParameter("total_amount"));
            System.out.println("买家编号: " + request.getParameter("buyer_id"));
            System.out.println("付款时间: " + request.getParameter("gmt_payment"));
            System.out.println("付款金额: " + request.getParameter("buyer_pay_amount"));
        }
        // 修改订单状态为已支付
        return orderService.updateStatusBySn(sn, ML.Order.PAID);
    }

    @Operation(summary = "查询 - 订单状态", description = "根据订单号查询订单状态（是否已支付）")
    @GetMapping("/checkStatusBySn/{sn}")
    public boolean checkStatusBySn(@PathVariable("sn") String sn) {
        return orderService.checkStatusBySn(sn);
    }







}
