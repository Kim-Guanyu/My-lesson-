package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "购物车分页DTO")
@Data
public class CartPageDTO extends PageDTO {
    @Schema(description = "用户 ID，用户表外键")
    private Long fkUserId;

    @Schema(description = "用户账号（冗余）")
    private String username;

    @Schema(description = "课程 ID，课程表外键")
    private Long fkCourseId;

    @Schema(description = "课程标题（冗余）")
    private String courseTitle;
}
