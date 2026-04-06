package com.mdkj.dto;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import com.alibaba.excel.enums.poi.VerticalAlignmentEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT, verticalAlignment = VerticalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT, verticalAlignment = VerticalAlignmentEnum.CENTER)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDetailExcelDTO implements Serializable {
    @ExcelProperty(value = {"订单明细表", "订单状态"})
    private String status;
    @ExcelProperty(value = {"订单明细表", "订单编号"})  
    private String sn;
    @ExcelProperty(value = {"订单明细表", "下单账号"})
    private String username;
    @ExcelProperty(value = {"订单明细表", "课程标题"})
    private String courseTitle;
    @ExcelProperty(value = {"订单明细表", "课程单价"})
    private Double coursePrice;
    @ExcelProperty(value = {"订单明细表", "订单总金额"})  
    private Double totalAmount;  
    @ExcelProperty(value = {"订单明细表", "实际支付总金额"})  
    private Double payAmount;  
    @ExcelProperty(value = {"订单明细表", "订单支付方式"})  
    private String payType;  
    @ExcelProperty(value = {"订单明细表", "创建时间"})
    private LocalDateTime created;
    @ExcelProperty(value = {"订单明细表", "修改时间"})
    private LocalDateTime updated;
    @ExcelProperty(value = {"订单明细表", "订单描述"})
    private String info;
}
