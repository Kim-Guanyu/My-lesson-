package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;


@EqualsAndHashCode(callSuper = true)  
@ToString(callSuper = true)  
@Schema(name = "评论分页DTO")
@Data
public class CommentPageDTO extends PageDTO {  
    @Range(min = 0, message = "父评论主键最小为0")
    @Schema(description = "父评论主键，0视为根节点")  
    private Long pid;  
    @Schema(description = "集次ID，集次表外键")  
    private Long fkEpisodeId;  
    @Schema(description = "用户ID，用户表外键")  
    private Long fkUserId;  
}
