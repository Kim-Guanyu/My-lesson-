package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;  
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.time.LocalDateTime;
  

@Schema(name = "秒杀活动添加DTO")
@Data
public class SeckillInsertDTO implements Serializable {
  
    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;
    
    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是一个未来的时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是一个未来的时间")
    private LocalDateTime endTime;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    @Range(min = 0, max = 2, message = "状态必须在0~2之间")
    private Integer status;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
