package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "收藏修改DTO")
@Data
public class FollowUpdateDTO implements Serializable {

    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "集次表ID，集次表外键")
    @NotNull(message = "集次表ID不能为空")
    private Long fkEpisodeId;

    @Schema(description = "用户表ID，用户表外键")
    @NotNull(message = "用户表ID不能为空")
    private Long fkUserId;
}
