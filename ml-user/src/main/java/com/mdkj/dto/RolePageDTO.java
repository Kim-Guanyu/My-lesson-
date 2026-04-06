package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "角色分页DTO")
@Data
public class RolePageDTO extends PageDTO {
    @Schema(description = "标题")
    private String title;
}
