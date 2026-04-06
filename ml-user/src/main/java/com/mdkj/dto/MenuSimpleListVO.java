package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "菜单全查VO")
@Data
public class MenuSimpleListVO implements Serializable {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "父菜单主键")
    private Long pid;
    @Schema(description = "父菜单标题")
    private String parentTitle;
}
