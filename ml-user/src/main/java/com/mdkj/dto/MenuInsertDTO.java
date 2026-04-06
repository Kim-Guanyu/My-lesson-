package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Schema(name = "菜单添加DTO")
@Data
public class MenuInsertDTO implements Serializable {

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    @Pattern(regexp = ML.Regex.TITLE_RE, message = ML.Regex.TITLE_RE_MSG)
    private String title;
    
    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;
    
    @Schema(description = "图标")
    @NotEmpty(message = "图标不能为空")
    @Pattern(regexp = ML.Regex.MENU_ICON_RE, message = ML.Regex.MENU_ICON_RE_MSG)
    private String icon;
    
    @Schema(description = "父菜单ID")
    @NotNull(message = "父菜单ID不能为空")
    @Min(value = 0, message = "父菜单ID不能小于0")
    private Long pid;
    
    @Schema(description = "地址")
    @NotEmpty(message = "地址不能为空")
    @Pattern(regexp = ML.Regex.MENU_URL_RE, message = ML.Regex.MENU_URL_RE_MSG)
    private String url;
    
    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;
}
