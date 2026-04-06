package com.mdkj.service;

import com.mdkj.dto.CategoryInsertDTO;
import com.mdkj.dto.CategoryPageDTO;
import com.mdkj.dto.CategorySimpleListVO;
import com.mdkj.dto.CategoryUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Category;

import java.util.List;

/**
 * 课程类别表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface CategoryService extends IService<Category> {

    boolean insert(CategoryInsertDTO dto);
    Category select(Long id);
    List<CategorySimpleListVO> simpleList();
    PageVO<Category> page(CategoryPageDTO dto);
    boolean update(CategoryUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);


}
