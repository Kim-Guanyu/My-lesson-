package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "用户全查VO")
@Data
public class UserSimpleListVO implements Serializable {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "账号")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
}
