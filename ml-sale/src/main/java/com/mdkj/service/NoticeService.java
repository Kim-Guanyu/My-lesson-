package com.mdkj.service;

import com.mdkj.dto.NoticeInsertDTO;
import com.mdkj.dto.NoticePageDTO;
import com.mdkj.dto.NoticeUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Notice;

import java.util.List;

/**
 * 通知表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface NoticeService extends IService<Notice> {
    boolean insert(NoticeInsertDTO dto);
    Notice select(Long id);
    PageVO<Notice> page(NoticePageDTO dto);
    boolean update(NoticeUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<Notice> top(Long n);


}
