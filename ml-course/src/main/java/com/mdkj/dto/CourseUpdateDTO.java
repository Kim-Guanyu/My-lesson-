package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;


@Schema(name = "课程修改DTO")
@Data
public class CourseUpdateDTO implements Serializable {
    
    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;

    @Schema(description = "作者")
    @NotEmpty(message = "作者不能为空")
    @Pattern(regexp = ML.Regex.AUTHOR_RE, message = ML.Regex.AUTHOR_RE_MSG)
    private String author;

    @Schema(description = "类别ID，类别表外键")
    @NotNull(message = "类别ID不能为空")
    private Long fkCategoryId;

    @Schema(description = "单价，单位元")
    @NotNull(message = "单价不能为空")
    @Range(min = 0, message = "单价最少为0")
    private Double price;

    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
