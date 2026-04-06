package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.CouponsInsertDTO;
import com.mdkj.dto.CouponsPageDTO;
import com.mdkj.dto.CouponsSimpleListVO;
import com.mdkj.dto.CouponsUpdateDTO;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Coupons;
import com.mdkj.mapper.CouponsMapper;
import com.mdkj.service.CouponsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.CouponsTableDef.COUPONS;

/**
 * 优惠卷表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class CouponsServiceImpl extends ServiceImpl<CouponsMapper, Coupons>  implements CouponsService{

    @Override
    public boolean insert(CouponsInsertDTO dto) {
        String title = dto.getTitle();
        String code = dto.getCode();

        // 判断生效时间和失效时间是否合理
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new ServiceException(ResultCode.DATETIME_ILLEGAL, "失效时间早于生效时间");
        }

        // 标题查重
        // select count(*) from coupons where title = ?
        if (QueryChain.of(mapper)
                .where(COUPONS.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 口令查重
        // select count(*) from coupons where code = ?
        if (QueryChain.of(mapper)
                .where(COUPONS.CODE.eq(code))
                .exists()) {
            throw new ServiceException(ResultCode.CODE_REPEAT, "口令" + code + "重复");
        }

        // 组装实体类
        Coupons coupons = BeanUtil.copyProperties(dto, Coupons.class);
        coupons.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        coupons.setCreated(LocalDateTime.now());
        coupons.setUpdated(LocalDateTime.now());
        // insert into coupons (code, title, cp_price, info, start_time, end_time, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(coupons) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Coupons select(Long id) {
        // select * from coupons where id = ?
        Coupons coupons = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(coupons)) {
            throw new ServiceException(ResultCode.COUPONS_NOT_FOUND, id + "号优惠卷数据不存在");
        }
        return coupons;
    }

    @Override
    public List<CouponsSimpleListVO> simpleList() {
        // select * from coupons
        return QueryChain.of(mapper)
                .listAs(CouponsSimpleListVO.class);
    }

    @Override
    public PageVO<Coupons> page(CouponsPageDTO dto) {
        QueryChain<Coupons> queryChain = QueryChain.of(mapper);

        // title 条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(COUPONS.TITLE.like(title));
        }

        // code 条件
        String code = dto.getCode();
        if (ObjectUtil.isNotNull(code)) {
            queryChain.where(COUPONS.CODE.like(code));
        }

        // DB分页并转为VO
        Page<Coupons> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Coupons> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(CouponsUpdateDTO dto) {
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        Long id = dto.getId();
        String title = dto.getTitle();
        String code = dto.getCode();

        // 检查优惠卷是否存在
        this.existsById(id);

        // 判断生效时间和失效时间是否合理
        if (ObjectUtil.isNotNull(startTime) && ObjectUtil.isNotNull(endTime) && startTime.isAfter(endTime)) {
            throw new ServiceException(ResultCode.DATETIME_ILLEGAL, "失效时间早于生效时间");
        }

        // 标题查重
        // select count(*) from coupons where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(COUPONS.TITLE.eq(title))
                .and(COUPONS.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 口令查重
        // select count(*) from coupons where code = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(COUPONS.CODE.eq(code))
                .and(COUPONS.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.CODE_REPEAT, "口令" + code + "重复");
        }

        // 组装实体类
        Coupons coupons = BeanUtil.copyProperties(dto, Coupons.class);
        coupons.setUpdated(LocalDateTime.now());
        // update coupons set code = ?, title = ?, cp_price = ?, info = ?, start_time = ?, end_time = ?, updated = ? where id = ?
        if (!UpdateChain.of(coupons)
                .where(COUPONS.ID.eq(coupons.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查优惠卷是否存在
        this.existsById(id);

        // delete from coupons where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查优惠卷是否存在
        // select count(*) from coupons where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(COUPONS.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.COUPONS_NOT_FOUND, "至少一个优惠卷数据不存在");
        }

        // delete from coupons where id in (?, ?, ?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查优惠卷是否存在，如果不存在则直接抛出异常
     *
     * @param id 优惠卷主键
     */
    private void existsById(Long id) {
        // select count(*) from coupons where id = ?
        if (!QueryChain.of(mapper)
                .where(COUPONS.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.COUPONS_NOT_FOUND, id + "号优惠卷数据不存在");
        }
    }

    @Override
    public Coupons selectByCode(String code) {
        // select * from coupons where code = ?
        return QueryChain.of(mapper)
                .where(COUPONS.CODE.eq(code))
                .one();
    }



}
