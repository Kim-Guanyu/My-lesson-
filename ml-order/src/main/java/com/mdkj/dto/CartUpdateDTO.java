package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "购物车修改DTO")
@Data
public class CartUpdateDTO implements Serializable {
  
    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "用户ID，用户表外键")
    @NotNull(message = "用户ID不能为空")
    private Long fkUserId;

    @Schema(description = "课程ID，课程表外键")
    @NotNull(message = "课程ID不能为空")
    private Long fkCourseId;
}
