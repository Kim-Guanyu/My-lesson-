package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;


@Schema(name = "订单添加DTO")
@Data
public class OrderInsertDTO implements Serializable {

	@Schema(description = "用户ID：购买人的ID")
	@NotNull(message = "用户ID不能为空")
	private Long fkUserId;

	@Schema(description = "总金额")
	@NotNull(message = "总金额不能为空")
	@DecimalMin(value = "0", message = "总金额不能小于0元")
	private Double totalAmount;

	@Schema(description = "实际支付总金额")
	@NotNull(message = "实际支付总金额不能为空")
	@DecimalMin(value = "0", message = "实际支付总金额不能小于0元")
	private Double payAmount;

	@Schema(description = "状态")
	@NotNull(message = "状态不能为空")
	@Range(min = 0, max = 3, message = "状态必须在0~3之间")
	private Integer status;

	@Schema(description = "支付方式")
	@NotNull(message = "支付方式不能为空")
	@Range(min = 0, max = 3, message = "支付方式必须在0~3之间")
	private Integer payType;

	@Schema(description = "描述")
	@Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
	private String info;

	@Schema(description = "优惠卷ID：下单时使用的优惠卷")
	private Long fkCouponsId;
}
