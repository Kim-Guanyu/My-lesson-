package com.mdkj.service;

import com.mdkj.dto.ArticleInsertDTO;
import com.mdkj.dto.ArticlePageDTO;
import com.mdkj.dto.ArticleUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Article;

import java.util.List;

/**
 * 新闻表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface ArticleService extends IService<Article> {
    boolean insert(ArticleInsertDTO dto);
    Article select(Long id);
    PageVO<Article> page(ArticlePageDTO dto);
    boolean update(ArticleUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<Article> top(Long n);


}
