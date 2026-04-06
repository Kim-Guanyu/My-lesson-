package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;  
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(name = "优惠卷添加DTO")
@Data
public class CouponsInsertDTO implements Serializable {
    
    @Schema(description = "口令")
    @NotEmpty(message = "口令不能为空")
    @Pattern(regexp = ML.Regex.CODE_RE, message = ML.Regex.CODE_RE_MSG)
    private String code;

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;

    @Schema(description = "优惠价格")
    @NotNull(message = "优惠价格不能为空")
    @Range(min = 0, message = "优惠价格最少为0")
    private Double cpPrice;

    @Schema(description = "生效时间")
    @Future(message = "生效时间必须是一个未来的时间")
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "失效时间")
    @Future(message = "失效时间必须是一个未来的时间")
    @NotNull(message = "失效时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;  
}
