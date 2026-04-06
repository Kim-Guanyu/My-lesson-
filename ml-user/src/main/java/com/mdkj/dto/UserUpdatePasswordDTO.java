package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;  
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/** @author 周航宇 */  
@Schema(name = "用户修改密码DTO")
@Data
public class UserUpdatePasswordDTO implements Serializable {
  
    @NotNull(message = "主键不能为空")  
    @Schema(description = "主键")  
    private Long id;  
  
    @Schema(description = "旧登录密码")  
    @NotEmpty(message = "旧密码不能为空")  
    @Pattern(regexp = ML.Regex.PASSWORD_RE, message = ML.Regex.PASSWORD_RE_MSG)
    private String oldPassword;  
  
    @Schema(description = "新登录密码")  
    @NotEmpty(message = "新密码不能为空")  
    @Pattern(regexp = ML.Regex.PASSWORD_RE, message = ML.Regex.PASSWORD_RE_MSG)  
    private String newPassword;  
}
