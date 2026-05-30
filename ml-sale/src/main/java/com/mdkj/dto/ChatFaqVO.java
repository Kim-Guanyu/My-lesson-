package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Schema(name = "客服常见问题VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatFaqVO implements Serializable {

    @Schema(description = "问题标题")
    private String question;

    @Schema(description = "快捷回复内容")
    private String answer;
}
