package com.mdkj.fallback;

import com.mdkj.entity.Course;
import com.mdkj.feign.CourseFeign;
import com.mdkj.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j  
@Component
public class CourseFeignFallback implements CourseFeign {
  
    @Override  
    public Result<Course> select(Long id) {
        log.error("课程微服务远程调用失败，请联系管理员。");  
        return null;  
    }  
}
