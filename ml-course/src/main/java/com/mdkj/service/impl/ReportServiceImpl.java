package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.nacos.api.model.v2.Result;
import com.mdkj.dto.ReportInsertDTO;
import com.mdkj.dto.ReportPageDTO;
import com.mdkj.dto.ReportUpdateDTO;
import com.mdkj.entity.User;
import com.mdkj.exception.ServiceException;
import com.mdkj.feign.UserFeign;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Report;
import com.mdkj.mapper.ReportMapper;
import com.mdkj.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.ReportTableDef.REPORT;

/**
 * 举报表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report>  implements ReportService{

    @Resource
    private UserFeign userFeign;

    @Override
    public boolean insert(ReportInsertDTO dto) {
        Long fkUserId = dto.getFkUserId();

        // 组装实体类
        Report report = BeanUtil.copyProperties(dto, Report.class);
        Result<User> userResult = userFeign.select(fkUserId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
        }
        report.setNickname(user.getNickname());
        report.setCreated(LocalDateTime.now());
        report.setUpdated(LocalDateTime.now());
        // insert into report (fk_episode_id, fk_user_id, nickname, content, created, updated) values (?, ?, ?, ?, ?, ?)
        if (mapper.insert(report) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Report select(Long id) {
        // 指定联查字段
        RelationManager.addQueryRelations("episode");

        // select * from report where id = ?
        Report report = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(report)) {
            throw new ServiceException(ResultCode.REPORT_NOT_FOUND, id + "号举报数据不存在");
        }
        return report;
    }

    @Override
    public PageVO<Report> page(ReportPageDTO dto) {
        // 指定联查字段
        RelationManager.addQueryRelations("episode");
        QueryChain<Report> queryChain = QueryChain.of(mapper);

        // episodeId条件
        if (ObjectUtil.isNotNull(dto.getFkEpisodeId())) {
            queryChain.where(REPORT.FK_EPISODE_ID.eq(dto.getFkEpisodeId()));
        }

        // userId条件
        if (ObjectUtil.isNotNull(dto.getFkUserId())) {
            queryChain.where(REPORT.FK_USER_ID.eq(dto.getFkUserId()));
        }

        // DB分页并转为VO
        Page<Report> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Report> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(ReportUpdateDTO dto) {
        Long id = dto.getId();
        Long fkUserId = dto.getFkUserId();

        // 检查举报是否存在
        this.existsById(id);

        // 组装实体类
        Report report = BeanUtil.copyProperties(dto, Report.class);
        Result<User> userResult = userFeign.select(fkUserId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
        }
        report.setNickname(user.getNickname());
        report.setUpdated(LocalDateTime.now());
        // update report set fk_episode_id = ?, fk_user_id = ?, nickname = ?, content = ?, updated = ? where id = ?
        if (!UpdateChain.of(report)
                .where(REPORT.ID.eq(report.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查举报是否存在
        this.existsById(id);

        // delete from report where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查举报是否存在
        // select count(*) from report where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(REPORT.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.REPORT_NOT_FOUND, "至少一个举报数据不存在");
        }

        // delete from report where id in (ids)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查举报是否存在，如果不存在则直接抛出异常
     *
     * @param id 举报主键
     */
    private void existsById(Long id) {
        // select count(*) from report where id = ?
        if (!QueryChain.of(mapper)
                .where(REPORT.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.REPORT_NOT_FOUND, id + "号举报数据不存在");
        }
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        // delete from report where fk_user_id = userId
        return UpdateChain.of(mapper)
                .where(REPORT.FK_USER_ID.eq(userId))
                .remove();
    }

    @Override
    public boolean deleteByUserIds(List<Long> userIds) {
        // delete from report where fk_user_id in (userIds)
        return UpdateChain.of(mapper)
                .where(REPORT.FK_USER_ID.in(userIds))
                .remove();
    }




}
