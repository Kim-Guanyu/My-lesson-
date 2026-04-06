package com.mdkj.dto;
import com.mdkj.util.ML;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Schema(name = "横幅添加DTO")
@Data
public class BannerInsertDTO implements Serializable {

	@Schema(description = "序号")
	@NotNull(message = "序号不能为空")
	@Range(min = 0, message = "序号最小为0")
	private Long idx;
	
	@Schema(description = "描述")
	@Pattern(regexp = ML.Regex.INFO_RE, message = ML.Regex.INFO_RE_MSG)
	private String info;
}
