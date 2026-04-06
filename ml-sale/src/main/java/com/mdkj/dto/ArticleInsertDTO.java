package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;


@Schema(name = "新闻添加DTO")
@Data
public class ArticleInsertDTO implements Serializable {

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;

    @Schema(description = "内容")
    @NotEmpty(message = "内容不能为空")
    @Pattern(regexp = ML.Regex.CONTENT_RE, message = ML.Regex.CONTENT_RE_MSG)
    private String content;

    @Schema(description = "序号")
	@NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;
}
