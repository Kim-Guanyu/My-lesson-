package com.mdkj.dao;

import com.mdkj.es.BarrageDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/** @author 周航宇 */
public interface BarrageRepository extends ElasticsearchRepository<BarrageDoc, Long> {

    /**
     * 根据视频ID分页查询弹幕记录，并按时间升序排序
     *
     * @param episodeId 视频集主键
     * @return 弹幕列表
     */
    List<BarrageDoc> findByEpisodeIdOrderByTime(String episodeId);
}
