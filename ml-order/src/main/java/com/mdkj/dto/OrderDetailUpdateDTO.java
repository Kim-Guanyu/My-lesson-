package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "订单明细修改DTO")
@Data
public class OrderDetailUpdateDTO implements Serializable {
  
	@Schema(description = "主键")  
	@NotNull(message = "主键不能为空")
	private Long id;

	@Schema(description = "订单ID，订单表外键")
	@NotNull(message = "订单主键不能为空")
	private Long fkOrderId;

	@Schema(description = "课程ID，课程表外键")
	@NotNull(message = "课程主键不能为空")
	private Long fkCourseId;
}
