package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Schema(name = "集次添加DTO")
@Data
public class EpisodeInsertDTO implements Serializable {

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;

    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;

    @Schema(description = "季次表ID，季次表外键")
    @NotNull(message = "季次表ID不能为空")
    private Long fkSeasonId;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
