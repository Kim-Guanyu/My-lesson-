package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.util.List;

@Schema(name = "用户修改DTO")
@Data
public class UserUpdateDTO implements Serializable {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    
    @Schema(description = "昵称")
    @NotEmpty(message = "昵称不能为空")
    @Pattern(regexp = ML.Regex.NICKNAME_RE, message = ML.Regex.NICKNAME_RE_MSG)
    private String nickname;

    @Schema(description = "邮箱")
    @NotEmpty(message = "邮箱不能为空")
    @Pattern(regexp = ML.Regex.EMAIL_RE, message = ML.Regex.EMAIL_RE_MSG)
    private String email;

    @Schema(description = "性别")
    @NotNull(message = "性别不能为空")
    @Range(min = 0, max = 2, message = "性别代码必须在0~2之间")
    private Integer gender;

    @Schema(description = "年龄")
    @NotNull(message = "年龄不能为空")
    @Range(min = 16, max = 60, message = "年龄必须在16~60之间")
    private Integer age;

    @Schema(description = "星座")
    @NotEmpty(message = "星座不能为空")
    private String zodiac;

    @Schema(description = "省份")
    @NotEmpty(message = "省份不能为空")
    @Pattern(regexp = ML.Regex.PROVINCE_RE, message = ML.Regex.PROVINCE_RE_MSG)
    private String province;

    @Schema(description = "描述")
    @NotEmpty(message = "描述不能为空")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
