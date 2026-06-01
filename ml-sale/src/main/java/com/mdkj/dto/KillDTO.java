package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "秒杀DTO")
@Data
public class KillDTO implements Serializable {

	@Schema(description = "活动ID")
	@NotNull(message = "活动ID不能为空")
	private Long fkSeckillId;

	@Schema(description = "课程ID")
	@NotNull(message = "课程ID不能为空")
	private Long fkCourseId;
}
