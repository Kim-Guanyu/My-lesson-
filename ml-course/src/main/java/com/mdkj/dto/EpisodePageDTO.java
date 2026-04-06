package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;  
  

@EqualsAndHashCode(callSuper = true)  
@ToString(callSuper = true)  
@Schema(name = "集次分页DTO")
@Data
public class EpisodePageDTO extends PageDTO {  
    @Schema(description = "标题")  
    private String title;  
    @Schema(description = "季次ID，季次表外键")  
    private Long fkSeasonId;  
}
