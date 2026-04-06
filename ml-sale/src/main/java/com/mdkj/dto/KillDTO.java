package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "秒杀DTO")
@Data
public class KillDTO implements Serializable {

	@Schema(description = "活动ID")
	@NotNull(message = "活动ID不能为空")
	private Long fkSeckillId;

	@Schema(description = "用户ID")
	@NotNull(message = "用户ID不能为空")
	private Long fkUserId;

	@Schema(description = "课程ID")
	@NotNull(message = "课程ID不能为空")
	private Long fkCourseId;

	@Schema(description = "课程价格")
	@NotNull(message = "课程价格不能为空")
	@DecimalMin(value = "0", message = "课程价格不能小于0元")
	private Double price;

	@Schema(description = "课程秒杀价格")
	@NotNull(message = "课程秒杀价格不能为空")
	@DecimalMin(value = "0", message = "课程秒杀价格不能小于0元")
	private Double skPrice;
}
