package com.mdkj.service;

import com.mdkj.dto.CommentInsertDTO;
import com.mdkj.dto.CommentPageDTO;
import com.mdkj.dto.CommentSimpleListVO;
import com.mdkj.dto.CommentUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Comment;

import java.util.List;

/**
 * 评论表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface CommentService extends IService<Comment> {
    boolean insert(CommentInsertDTO dto);
    Comment select(Long id);
    List<CommentSimpleListVO> simpleList();
    PageVO<Comment> page(CommentPageDTO dto);
    boolean update(CommentUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    boolean deleteByUserId(Long userId);
    boolean deleteByUserIds(List<Long> userIds);


}
