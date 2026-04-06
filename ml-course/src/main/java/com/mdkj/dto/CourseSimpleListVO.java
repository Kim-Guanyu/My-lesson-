package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "课程全查VO")
@Data
public class CourseSimpleListVO implements Serializable {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "单价")
    private Double price;
}
