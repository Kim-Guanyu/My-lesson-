package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "订单明细添加DTO")
@Data
public class OrderDetailInsertDTO implements Serializable {
  
	@Schema(description = "订单ID，订单表外键")
	@NotNull(message = "订单主键不能为空")
	private Long fkOrderId;
	
	@Schema(description = "课程ID，课程表外键")
	@NotNull(message = "课程主键不能为空")
	private Long fkCourseId;
}
