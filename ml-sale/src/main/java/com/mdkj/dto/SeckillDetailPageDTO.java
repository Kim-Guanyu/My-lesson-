package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;  

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "秒杀活动明细分页DTO")
@Data
public class SeckillDetailPageDTO extends PageDTO {
	@Schema(description = "秒杀活动主键")
	private Long seckillId;		
	@Schema(description = "课程标题")
	private String courseTitle;
}
