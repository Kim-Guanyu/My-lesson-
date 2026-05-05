package com.mdkj.dao;

import com.mdkj.es.BarrageDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BarrageRepository extends ElasticsearchRepository<BarrageDoc, String> {

}
