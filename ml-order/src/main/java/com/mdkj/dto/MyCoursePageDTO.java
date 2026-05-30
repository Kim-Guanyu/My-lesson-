package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "我的课程分页DTO")
@Data
public class MyCoursePageDTO extends PageDTO {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long fkUserId;
}
