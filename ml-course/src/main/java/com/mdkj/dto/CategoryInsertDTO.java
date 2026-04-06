package com.mdkj.dto;
import co.elastic.clients.elasticsearch._types.mapping.MatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import com.mdkj.util.ML;
import java.io.Serializable;




@Schema(name = "类别添加DTO")
@Data
public class CategoryInsertDTO implements Serializable {

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;

    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
