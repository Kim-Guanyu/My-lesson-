package com.mdkj.dao;

import com.mdkj.es.CourseDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CourseRepository extends ElasticsearchRepository<CourseDoc, Long> {

    /**
     * 根据课程名或作者名分页搜索记录，并按 idx 字段升序
     *
     * @param title  课程名称
     * @param author 作者名称
     * @return 课程列表
     */
    Page<CourseDoc> searchByTitleOrAuthorOrderByIdx(String title, String author, Pageable pageable);
}
