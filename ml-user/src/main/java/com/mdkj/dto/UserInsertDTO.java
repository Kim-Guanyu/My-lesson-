package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;


@Schema(name = "用户添加DTO")
@Data
public class UserInsertDTO implements Serializable {

    @Schema(description = "账号")
    @NotEmpty(message = "账号不能为空")
    @Pattern(regexp = ML.Regex.USERNAME_RE, message = ML.Regex.USERNAME_RE_MSG)
    private String username;

    @Schema(description = "密码")
    @NotEmpty(message = "密码不能为空")
    @Pattern(regexp = ML.Regex.PASSWORD_RE, message = ML.Regex.PASSWORD_RE_MSG)
    private String password;

    @Schema(description = "姓名")
    @NotEmpty(message = "姓名不能为空")
    @Pattern(regexp = ML.Regex.REALNAME_RE, message = ML.Regex.REALNAME_RE_MSG)
    private String realname;

    @Schema(description = "身份证号")
    @NotEmpty(message = "身份证号不能为空")
    @Pattern(regexp = ML.Regex.ID_CARD_RE, message = ML.Regex.ID_CARD_RE_MSG)
    private String idcard;

    @Schema(description = "手机号码")
    @NotEmpty(message = "手机号码不能为空")
    @Pattern(regexp = ML.Regex.PHONE_RE, message = ML.Regex.PHONE_RE_MSG)
    private String phone;

    @Schema(description = "邮箱")
    @NotEmpty(message = "邮箱不能为空")
    @Pattern(regexp = ML.Regex.EMAIL_RE, message = ML.Regex.EMAIL_RE_MSG)
    private String email;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
