package com.mdkj.feign;

import com.mdkj.entity.Course;
import com.mdkj.fallback.CourseFeignFallback;
import com.mdkj.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "ml-course", fallback = CourseFeignFallback.class)
public interface CourseFeign {  
  
    @GetMapping("/api/v1/course/select/{id}")
    Result<Course> select(@PathVariable("id") Long id);
}
