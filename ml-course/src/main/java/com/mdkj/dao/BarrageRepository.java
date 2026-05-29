package com.mdkj.dao;

import com.mdkj.es.BarrageDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;


public interface BarrageRepository extends ElasticsearchRepository<BarrageDoc, String> {

    /**
     * 根据视频ID分页查询弹幕记录，并按时间升序排序
     *
     * @param episodeId 视频集主键
     * @return 弹幕列表
     */
    List<BarrageDoc> findByEpisodeIdOrderByTime(String episodeId);

    /**
     * 根据多个视频/课程 ID 查询弹幕记录
     *
     * @param episodeIds 视频集或课程主键集合
     * @return 弹幕列表
     */
    List<BarrageDoc> findByEpisodeIdIn(Collection<String> episodeIds);
}
