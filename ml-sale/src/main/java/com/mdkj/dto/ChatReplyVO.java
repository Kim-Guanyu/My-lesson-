package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "客服回复VO")
@Data
public class ChatReplyVO implements Serializable {

    @Schema(description = "回复内容")
    private String reply;

    @Schema(description = "是否匹配到已知问题")
    private Boolean matched;
}
