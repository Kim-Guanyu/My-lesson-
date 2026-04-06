package com.mdkj.es;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.annotation.Id;  
import org.springframework.data.elasticsearch.annotations.Field;

/** @author 周航宇 */
@Data
@Document(indexName = "ml-barrage")
public class BarrageDoc {
    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private String episodeId;
    @Field(type = FieldType.Keyword)
    private String text;
    @Field(type = FieldType.Keyword)
    private String color;
    @Field(type = FieldType.Keyword)
    private String time;
}
