package com.mdkj.service;

import com.mdkj.dto.CourseInsertDTO;
import com.mdkj.dto.CoursePageDTO;
import com.mdkj.dto.CourseSimpleListVO;
import com.mdkj.dto.CourseUpdateDTO;
import com.mdkj.es.CourseDoc;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Course;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface CourseService extends IService<Course> {

    boolean insert(CourseInsertDTO dto);
    Course select(Long id);
    List<CourseSimpleListVO> simpleList();
    PageVO<Course> page(CoursePageDTO dto);
    boolean update(CourseUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);

    String uploadCover(MultipartFile newFile, Long id);

    String uploadSummary(MultipartFile newFile, Long id);

    PageVO<CourseDoc> search(CoursePageDTO dto);


}
