package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.SeasonInsertDTO;
import com.mdkj.dto.SeasonPageDTO;
import com.mdkj.dto.SeasonSimpleListVO;
import com.mdkj.dto.SeasonUpdateDTO;
import com.mdkj.exception.ServiceException;
import com.mdkj.mapper.EpisodeMapper;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Season;
import com.mdkj.mapper.SeasonMapper;
import com.mdkj.service.SeasonService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.EpisodeTableDef.EPISODE;
import static com.mdkj.entity.table.SeasonTableDef.SEASON;

/**
 * 季次表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class SeasonServiceImpl extends ServiceImpl<SeasonMapper, Season>  implements SeasonService{

    @Resource
    private EpisodeMapper episodeMapper;

    @Override
    public boolean insert(SeasonInsertDTO dto) {
        String title = dto.getTitle();

        // 标题查重
        // select count(*) from season where title = ?
        if (QueryChain.of(mapper)
                .where(SEASON.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Season season = BeanUtil.copyProperties(dto, Season.class);
        season.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        season.setCreated(LocalDateTime.now());
        season.setUpdated(LocalDateTime.now());

        // insert into season (title, info, idx, fk_course_id, created, updated) values (?, ?, ?, ?, ?, ?)
        if (mapper.insert(season) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Season select(Long id) {

        // 指定联查字段
        RelationManager.addQueryRelations("course", "category", "episodes");

        // select * from season where id = ?
        Season season = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(season)) {
            throw new ServiceException(ResultCode.SEASON_NOT_FOUND, id + "号季次记录不存在");
        }
        return season;
    }

    @Override
    public List<SeasonSimpleListVO> simpleList() {
        // select * from season order by idx asc, id desc
        return QueryChain.of(mapper)
                .orderBy(SEASON.IDX.asc(), SEASON.ID.desc())
                .withRelations()
                .listAs(SeasonSimpleListVO.class);
    }

    @Override
    public PageVO<Season> page(SeasonPageDTO dto) {
        QueryChain<Season> queryChain = QueryChain.of(mapper)
                .orderBy(SEASON.IDX.asc(), SEASON.ID.desc());

        // title条件
        if (ObjectUtil.isNotEmpty(dto.getTitle())) {
            queryChain.where(SEASON.TITLE.like(dto.getTitle()));
        }

        // fkCourseId条件
        if (ObjectUtil.isNotEmpty(dto.getFkCourseId())) {
            queryChain.where(SEASON.FK_COURSE_ID.eq(dto.getFkCourseId()));
        }

        // DB分页并转为VO
        Page<Season> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Season> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(SeasonUpdateDTO dto) {
        String title = dto.getTitle();
        Long id = dto.getId();

        // 检查季次是否存在
        this.existsById(id);

        // 标题查重
        // select count(*) from season where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(SEASON.TITLE.eq(title))
                .and(SEASON.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Season season = BeanUtil.copyProperties(dto, Season.class);
        season.setUpdated(LocalDateTime.now());

        // update season set title = ?, info = ?, idx = ?, fk_course_id = ?, updated = ? where id = ?
        if (!UpdateChain.of(season)
                .where(SEASON.ID.eq(season.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id) {

        // 检查季次是否存在
        this.existsById(id);

        // 通过季次主键查询全部集次的ID列表
        // select id from episode where fk_season_id = #{id}
        List<Long> episodeIds = QueryChain.of(episodeMapper)
                .select(EPISODE.ID)
                .where(EPISODE.FK_SEASON_ID.eq(id))
                .objListAs(Long.class);

        // 存在集次时，批量删除集次
        this.clearEpisode(episodeIds);

        // 删除季次
        // delete from season where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除季次记录失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查季次是否存在
        // select count(*) from season where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(SEASON.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.SEASON_NOT_FOUND, "至少一个季次数据不存在");
        }

        // 通过季次主键列表查询全部集次的ID列表
        // select id from episode where fk_season_id in (>)
        List<Long> episodeIds = QueryChain.of(episodeMapper)
                .select(EPISODE.ID)
                .where(EPISODE.FK_SEASON_ID.in(ids))
                .objListAs(Long.class);

        // 存在集次时，批量删除集次
        this.clearEpisode(episodeIds);

        // 批量删除季次
        // delete from season where id in (>)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除季次记录失败");
        }
        return true;
    }

    /**
     * 按主键检查季次是否存在，如果不存在则直接抛出异常
     *
     * @param id 季次主键
     */
    private void existsById(Long id) {
        // select count(*) from season where id = ?
        if (!QueryChain.of(mapper)
                .where(SEASON.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.SEASON_NOT_FOUND, id + "号季次数据不存在");
        }
    }

    /**
     * 根据集次ID列表，清空全部集次记录
     *
     * @param episodeIds 集次主键列表
     */
    private void clearEpisode(List<Long> episodeIds) {
        // 存在集记录时，批量删除集
        if (ObjectUtil.isNotEmpty(episodeIds)) {
            if (episodeMapper.deleteBatchByIds(episodeIds) <= 0) {
                throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除集次记录失败");
            }
        }
    }



}
