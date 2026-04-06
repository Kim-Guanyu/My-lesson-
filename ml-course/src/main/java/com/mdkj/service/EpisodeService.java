package com.mdkj.service;

import com.mdkj.dto.*;
import com.mdkj.es.BarrageDoc;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Episode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 集次表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface EpisodeService extends IService<Episode> {
    boolean insert(EpisodeInsertDTO dto);
    Episode select(Long id);
    List<EpisodeSimpleListVO> simpleList();
    PageVO<Episode> page(EpisodePageDTO dto);
    boolean update(EpisodeUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    String uploadVideoCover(MultipartFile newFile, Long id);

    String uploadVideo(MultipartFile newFile, Long id);
    List<EpisodeExcelDTO> getExcelData();
    List<BarrageDoc> searchBarrage(String episodeId);


}
