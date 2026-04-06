package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)  
@ToString(callSuper = true)  
@Schema(name = "订单分页DTO")  
@Data
public class OrderPageDTO extends PageDTO {  
    @Schema(description = "编号")
    private String sn;
    @Schema(description = "状态")
    @Range(min = 0, max = 3, message = "状态代码必须在0~3之间")
    private Integer status;
    @Schema(description = "用户账号，冗余字段")
    private String username;
}
