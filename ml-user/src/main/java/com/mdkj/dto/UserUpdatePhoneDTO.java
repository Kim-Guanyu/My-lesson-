package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "用户修改手机号码DTO")
@Data
public class UserUpdatePhoneDTO implements Serializable {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @NotNull(message = "手机号码不能为空")
    @Pattern(regexp = ML.Regex.PHONE_RE, message = ML.Regex.PHONE_RE_MSG)
    @Schema(description = "手机号码")
    private String phone;

    @NotNull(message = "验证码不能为空")
    @Pattern(regexp = ML.Regex.VCODE_RE, message = ML.Regex.VCODE_RE_MSG)
    @Schema(description = "验证码")
    private String vcode;
}
