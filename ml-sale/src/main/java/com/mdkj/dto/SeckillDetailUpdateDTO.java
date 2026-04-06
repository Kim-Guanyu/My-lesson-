package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "秒杀活动明细修改DTO")
@Data
public class SeckillDetailUpdateDTO implements Serializable {

    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "秒杀活动ID，秒杀活动表外键")
    @NotNull(message = "秒杀活动主键不能为空")
    private Long fkSeckillId;

    @Schema(description = "课程ID，课程表外键")
    @NotNull(message = "课程主键不能为空")
    private Long fkCourseId;

    @Schema(description = "秒杀价格，单位：元")
    @NotNull(message = "秒杀价格不能为空")
    private Double skPrice;

    @Schema(description = "秒杀数量")
    @NotNull(message = "秒杀数量不能为空")
    private Long skCount;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
