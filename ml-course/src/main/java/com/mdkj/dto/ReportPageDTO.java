package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;  
  
/** @author 周航宇 */  
@EqualsAndHashCode(callSuper = true)  
@ToString(callSuper = true)  
@Schema(name = "举报分页DTO")
@Data
public class ReportPageDTO extends PageDTO {  
    @Schema(description = "集次ID，集次表外键")  
    private Long fkEpisodeId;  
    @Schema(description = "用户ID，用户表外键")  
    private Long fkUserId;  
}
