package com.mdkj.service;

import com.mdkj.dto.BannerInsertDTO;
import com.mdkj.dto.BannerPageDTO;
import com.mdkj.dto.BannerUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 横幅表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface BannerService extends IService<Banner> {
    boolean insert(BannerInsertDTO dto);
    Banner select(Long id);
    PageVO<Banner> page(BannerPageDTO dto);
    boolean update(BannerUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<Banner> top(Long n);
    String uploadBanner(MultipartFile newFile, Long id);

    /**
     * 列出 MinIO banner 目录下的所有图片文件名，供运营后台换图时选择。
     */
    List<String> minioImages();


}
