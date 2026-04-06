package com.mdkj.service;

import com.mdkj.dto.FollowInsertDTO;
import com.mdkj.dto.FollowPageDTO;
import com.mdkj.dto.FollowUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Follow;

import java.util.List;

/**
 * 收藏表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface FollowService extends IService<Follow> {
    boolean insert(FollowInsertDTO dto);
    Follow select(Long id);
    PageVO<Follow> page(FollowPageDTO dto);
    boolean update(FollowUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);


}
