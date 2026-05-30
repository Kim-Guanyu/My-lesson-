package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "客服提问DTO")
@Data
public class ChatAskDTO implements Serializable {

    @Schema(description = "用户提问内容")
    @NotBlank(message = "提问内容不能为空")
    private String question;

    @Schema(description = "用户ID，登录后可查询订单等信息")
    private Long fkUserId;

    @Schema(description = "当前咨询的课程ID")
    private Long courseId;

    @Schema(description = "当前咨询的课程标题")
    private String courseTitle;
}
