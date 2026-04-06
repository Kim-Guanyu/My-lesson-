package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;  
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Schema(name = "秒杀活动修改DTO")
@Data
public class SeckillUpdateDTO implements Serializable {
  
    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

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

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
