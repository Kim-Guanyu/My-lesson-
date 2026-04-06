package com.mdkj.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(name = "通知分页DTO")
@Data
public class NoticePageDTO extends PageDTO {}
