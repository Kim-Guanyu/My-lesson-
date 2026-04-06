package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "类别全查VO")
@Data
public class CategorySimpleListVO implements Serializable {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "标题")
    private String title;
}
