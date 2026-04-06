package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "用户登录DTO", description = "通过手机验证码登录")
@Data
public class LoginByPhoneDTO implements Serializable {

	@NotEmpty(message = "手机号不能为空")  
	@Pattern(regexp = ML.Regex.PHONE_RE, message = ML.Regex.PHONE_RE_MSG)
	@Schema(description = "手机号码")
	private String phone;
	
    @NotEmpty(message = "验证码不能为空")
    @Pattern(regexp = ML.Regex.VCODE_RE, message = ML.Regex.VCODE_RE_MSG)
	@Schema(description = "验证码")
    private String vcode;
}
