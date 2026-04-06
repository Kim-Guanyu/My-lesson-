package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "查询预支付二维码DTO")
@Data
public class QrCodeDTO implements Serializable {

	@Schema(description = "订单编号")
	@NotEmpty(message = "订单编号不能为空")
	private String sn;

	@Schema(description = "实际支付金额")  
	@NotNull(message = "实际支付金额不能为空")  
	private Double payAmount;
}
