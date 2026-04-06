package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "课程分页DTO")
@Data
public class CoursePageDTO extends PageDTO {
    @Schema(description = "标题")
    private String title;
    @Schema(description = "类别ID，类别表外键")
    private Long fkCategoryId;
    @Size(max = 42, message = "搜索关键字不能超过42个字")
	@Schema(description = "搜索关键字，比如课程名称或作者名称")
	private String keyword;
}
