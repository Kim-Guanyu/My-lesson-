package com.mdkj.service;

import com.mdkj.dto.SeasonInsertDTO;
import com.mdkj.dto.SeasonPageDTO;
import com.mdkj.dto.SeasonSimpleListVO;
import com.mdkj.dto.SeasonUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Season;

import java.util.List;

/**
 * 季次表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface SeasonService extends IService<Season> {
    boolean insert(SeasonInsertDTO dto);
    Season select(Long id);
    List<SeasonSimpleListVO> simpleList();
    PageVO<Season> page(SeasonPageDTO dto);
    boolean update(SeasonUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);

}
