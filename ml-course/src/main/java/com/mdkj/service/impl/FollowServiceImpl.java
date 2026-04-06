package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.nacos.api.model.v2.Result;
import com.mdkj.dto.FollowInsertDTO;
import com.mdkj.dto.FollowPageDTO;
import com.mdkj.dto.FollowUpdateDTO;
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
import com.mdkj.entity.Follow;
import com.mdkj.mapper.FollowMapper;
import com.mdkj.service.FollowService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.FollowTableDef.FOLLOW;

/**
 * 收藏表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>  implements FollowService{
    @Resource
    private UserFeign userFeign;

    @Override
    public boolean insert(FollowInsertDTO dto) {
        Long fkUserId = dto.getFkUserId();

        // 组装实体类
        Follow follow = BeanUtil.copyProperties(dto, Follow.class);
        Result<User> userResult = userFeign.select(fkUserId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
        }
        follow.setNickname(user.getNickname());
        follow.setCreated(LocalDateTime.now());
        follow.setUpdated(LocalDateTime.now());

        // insert into follow (fk_episode_id, fk_user_id, nickname, created, updated) values (?, ?, ?, ?, ?)
        if (mapper.insert(follow) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Follow select(Long id) {
        // 指定联查字段
        RelationManager.addQueryRelations("episode");

        // select * from follow where id = ?
        Follow follow = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(follow)) {
            throw new ServiceException(ResultCode.FOLLOW_NOT_FOUND, id + "号收藏数据不存在");
        }
        return follow;
    }

    @Override
    public PageVO<Follow> page(FollowPageDTO dto) {
        // 指定联查字段
        RelationManager.addQueryRelations("episode");
        QueryChain<Follow> queryChain = QueryChain.of(mapper);

        // episodeId条件
        if (ObjectUtil.isNotNull(dto.getFkEpisodeId())) {
            queryChain.where(FOLLOW.FK_EPISODE_ID.eq(dto.getFkEpisodeId()));
        }

        // userId条件
        if (ObjectUtil.isNotNull(dto.getFkUserId())) {
            queryChain.where(FOLLOW.FK_USER_ID.eq(dto.getFkUserId()));
        }

        // DB分页并转为VO
        Page<Follow> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Follow> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(FollowUpdateDTO dto) {

        Long id = dto.getId();
        Long fkUserId = dto.getFkUserId();

        // 检查收藏是否存在
        this.existsById(id);

        // 组装实体类
        Follow follow = BeanUtil.copyProperties(dto, Follow.class);
        Result<User> userResult = userFeign.select(fkUserId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
        }
        follow.setNickname(user.getNickname());
        follow.setUpdated(LocalDateTime.now());
        // update follow set fk_episode_id = ?, fk_user_id = ?, nickname = ?, content = ?, updated = ? where id = ?
        if (!UpdateChain.of(follow)
                .where(FOLLOW.ID.eq(follow.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {
        // 检查收藏是否存在
        this.existsById(id);

        // delete from follow where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        // 检查收藏是否存在
        // select count(*) from follow where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(FOLLOW.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.FOLLOW_NOT_FOUND, "至少一个收藏数据不存在");
        }

        // delete from follow where id in (ids)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查收藏是否存在，如果不存在则直接抛出异常
     *
     * @param id 收藏主键
     */
    private void existsById(Long id) {
        // select count(*) from follow where id = ?
        if (!QueryChain.of(mapper)
                .where(FOLLOW.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.FOLLOW_NOT_FOUND, id + "号收藏数据不存在");
        }
    }


}
