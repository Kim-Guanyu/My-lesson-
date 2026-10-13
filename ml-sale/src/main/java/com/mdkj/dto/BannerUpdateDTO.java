package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Schema(name = "横幅修改DTO")
@Data
public class BannerUpdateDTO implements Serializable {
  
    @Schema(description = "主键")
    @NotNull(message = "主键不能为空")
    private Long id;

    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    @Range(min = 0, message = "序号最小为0")
    private Long idx;

    @Schema(description = "描述")
    @Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
    private String info;

    /**
     * 图片文件名（MinIO banner 目录下的对象名，不含目录前缀）。
     * 运营后台「换图」时从已有图片中选一张传入；为空则不改动原图片。
     */
    @Schema(description = "图片文件名，取自 MinIO banner 目录；为空表示不修改")
    private String url;
}
