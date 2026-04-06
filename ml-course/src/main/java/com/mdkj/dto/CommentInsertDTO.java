package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "评论添加DTO")
@Data
public class CommentInsertDTO implements Serializable {

    @Schema(description = "集次表ID，季次表外键")
    @NotNull(message = "集次表ID不能为空")
    private Long fkEpisodeId;

    @Schema(description = "用户表ID，用户表外键")
    @NotNull(message = "用户表ID不能为空")
    private Long fkUserId;

    @Schema(description = "父评论ID，0视为根评论")
    @NotNull(message = "父评论ID不能为空")
    private Long pid;

    @Schema(description = "评论内容")
    @NotEmpty(message = "评论内容不能为空")
    @Pattern(regexp = ML.Regex.CONTENT_RE, message = ML.Regex.CONTENT_RE_MSG)
    private String content;
}
