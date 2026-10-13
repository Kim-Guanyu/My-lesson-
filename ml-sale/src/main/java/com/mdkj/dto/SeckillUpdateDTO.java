package com.mdkj.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    // 运营后台的 el-date-picker 送的是「2026-09-10 14:00:00」这种带空格的格式，
    // 而 Jackson 默认只认 ISO-8601（日期与时间之间必须是 T），不指定 pattern 会在空格处解析失败。
    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是一个未来的时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是一个未来的时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
