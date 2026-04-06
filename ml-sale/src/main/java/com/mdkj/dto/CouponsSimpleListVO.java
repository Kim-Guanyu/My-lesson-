package com.mdkj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(name = "优惠卷全查VO")
@Data
public class CouponsSimpleListVO implements Serializable {
    @Schema(description = "主键")  
    private Long id;  
    @Schema(description = "标题")  
    private String title;  
}
