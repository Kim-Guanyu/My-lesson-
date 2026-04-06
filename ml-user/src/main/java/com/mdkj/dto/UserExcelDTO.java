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
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserExcelDTO implements Serializable {
    @ExcelProperty(value = {"用户表", "登录账号"})
    private String username;
    @ExcelProperty(value = {"用户表", "创建时间"})
    private LocalDateTime created;
    @ExcelProperty(value = {"用户表", "修改时间"})
    private LocalDateTime updated;
    @ExcelProperty(value = {"用户表", "用户昵称"})
    private String nickname;
    @ExcelProperty(value = {"用户表", "手机号码"})
    private String phone;
    @ExcelProperty(value = {"用户表", "邮箱地址"})
    private String email;
    @ExcelProperty(value = {"用户表", "用户性别"})
    private String gender;
    @ExcelProperty(value = {"用户表", "用户年龄"})
    private Integer age;
    @ExcelProperty(value = {"用户表", "用户星座"})
    private String zodiac;
    @ExcelProperty(value = {"用户表", "用户省份"})
    private String province;
    @ExcelProperty(value = {"用户表", "个人描述"})
    private String info;
    @ExcelProperty(value = {"用户表", "真实姓名"})
    private String realname;
    @ExcelProperty(value = {"用户表", "身份证号"})
    private String idcard;
}
