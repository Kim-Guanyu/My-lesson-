package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


@Schema(description = "分页专用DTO")
@Data
public class PageDTO implements Serializable {

    @Min(value = 0, message = "当前第几页必须大于0")
    @NotNull(message = "当前第几页不能为空")
    @Schema(description = "当前第几页")
    private Integer pageNum;

    @Min(value = 0, message = "每页多少条必须大于0")
    @NotNull(message = "每页多少条不能为空")
    @Schema(description = "每页多少条")
    private Integer pageSize;
}
