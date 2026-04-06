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
public class EpisodeExcelDTO implements Serializable {
    @ExcelProperty(value = {"集次表", "视频类别"})
    private String categoryTitle;
    @ExcelProperty(value = {"集次表", "课程标题"})
    private String courseTitle;
    @ExcelProperty(value = {"集次表", "季次标题"})
    private String seasonTitle;
    @ExcelProperty(value = {"集次表", "视频标题"})
    private String title;
    @ExcelProperty(value = {"集次表", "视频作者"})
    private String author;
    @ExcelProperty(value = {"集次表", "视频单价（元）"})
    private Double price;
    @ExcelProperty(value = {"集次表", "创建时间"})
    private LocalDateTime created;
    @ExcelProperty(value = {"集次表", "修改时间"})
    private LocalDateTime updated;
    @ExcelProperty(value = {"集次表", "视频描述"})
    private String info;
    @ExcelProperty(value = {"集次表", "季次描述"})
    private String seasonInfo;
    @ExcelProperty(value = {"集次表", "课程描述"})
    private String courseInfo;
}
