package com.mdkj.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;


@Data
@Document(indexName = "ml-course")
public class CourseDoc {
    @Id
    private Long id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String author;
    @Field(type = FieldType.Keyword)
    private String cover;
    @Field(value = "category_title", type = FieldType.Keyword)
    private String categoryTitle;
    @Field(type = FieldType.Double)
    private Double price;
	@Field(type = FieldType.Long)  
	private Long idx;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updated;
}
