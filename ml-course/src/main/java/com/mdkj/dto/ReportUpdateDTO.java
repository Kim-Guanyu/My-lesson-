package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/** @author 周航宇 */
@Schema(name = "举报修改DTO")
@Data
public class ReportUpdateDTO implements Serializable {

    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "集次表ID，集次表外键")
    @NotNull(message = "集次表ID不能为空")
    private Long fkEpisodeId;

    @Schema(description = "用户表ID，用户表外键")
    @NotNull(message = "用户表ID不能为空")
    private Long fkUserId;

    @Schema(description = "举报内容")
    @NotEmpty(message = "举报内容不能为空")
    @Pattern(regexp = ML.Regex.CONTENT_RE, message = ML.Regex.CONTENT_RE_MSG)
    private String content;
}
