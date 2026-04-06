package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.SeckillDetailInsertDTO;
import com.mdkj.dto.SeckillDetailPageDTO;
import com.mdkj.dto.SeckillDetailUpdateDTO;
import com.mdkj.entity.Course;
import com.mdkj.exception.ServiceException;
import com.mdkj.feign.CourseFeign;
import com.mdkj.util.ML;
import com.mdkj.util.Result;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.SeckillDetail;
import com.mdkj.mapper.SeckillDetailMapper;
import com.mdkj.service.SeckillDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.SeckillDetailTableDef.SECKILL_DETAIL;

/**
 * 秒杀明细表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class SeckillDetailServiceImpl extends ServiceImpl<SeckillDetailMapper, SeckillDetail>  implements SeckillDetailService{

    @Resource
    private CourseFeign courseFeign;

    @Override
    public boolean insert(SeckillDetailInsertDTO dto) {
        Long fkCourseId = dto.getFkCourseId();

        // 远程调用 - 查询课程数据
        Result<Course> courseResult = courseFeign.select(fkCourseId);
        if (ObjectUtil.isNull(courseResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
        }
        Course course = courseResult.getData();
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, fkCourseId + "号课程数据不存在");
        }

        String courseTitle = course.getTitle();

        // 秒杀详情记录查重
        // select count(*) from seckill_detail where fk_seckill_id = ? and fk_course_id = ?
        fkCourseId = dto.getFkCourseId();
        if (QueryChain.of(mapper)
                .where(SECKILL_DETAIL.FK_SECKILL_ID.eq(dto.getFkSeckillId()))
                .and(SECKILL_DETAIL.FK_COURSE_ID.eq(fkCourseId))
                .exists()) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_REPEAT, "秒杀明细商品" + courseTitle + "重复");
        }

        // 组装实体类
        SeckillDetail seckillDetail = BeanUtil.copyProperties(dto, SeckillDetail.class);
        seckillDetail.setCourseTitle(courseTitle);
        seckillDetail.setCourseCover(course.getCover());
        seckillDetail.setCoursePrice(course.getPrice());
        seckillDetail.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        seckillDetail.setCreated(LocalDateTime.now());
        seckillDetail.setUpdated(LocalDateTime.now());
        // insert into seckill_detail (fk_seckill_id, fk_course_id, course_title, course_cover, course_price, sk_price, sk_count, info, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(seckillDetail) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public SeckillDetail select(Long id) {
        // select * from seckill_detail where id = ?
        SeckillDetail seckillDetail = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(seckillDetail)) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_NOT_FOUND, id + "号秒杀明细数据不存在");
        }
        return seckillDetail;
    }

    @Override
    public PageVO<SeckillDetail> page(SeckillDetailPageDTO dto) {
        QueryChain<SeckillDetail> queryChain = QueryChain.of(mapper);

        // seckillId条件
        Long seckillId = dto.getSeckillId();
        if (ObjectUtil.isNotNull(seckillId)) {
            queryChain.where(SECKILL_DETAIL.FK_SECKILL_ID.eq(seckillId));
        }

        // courseTitle条件
        String courseTitle = dto.getCourseTitle();
        if (ObjectUtil.isNotNull(courseTitle)) {
            queryChain.where(SECKILL_DETAIL.COURSE_TITLE.like(courseTitle));
        }

        // DB分页并转为VO
        Page<SeckillDetail> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<SeckillDetail> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(SeckillDetailUpdateDTO dto) {
        Long fkCourseId = dto.getFkCourseId();
        Long id = dto.getId();

        // 检查秒杀明细是否存在
        this.existsById(id);

        // 远程调用 - 查询课程数据
        Result<Course> courseResult = courseFeign.select(fkCourseId);
        if (ObjectUtil.isNull(courseResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
        }
        Course course = (Course) courseResult.getData();
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, fkCourseId + "号课程数据不存在");
        }

        String courseTitle = course.getTitle();

        // 课程查重
        // select count(*) from seckill_detail where fk_course_id = ? and fk_seckill_id = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(SECKILL_DETAIL.FK_COURSE_ID.eq(dto.getFkCourseId()))
                .and(SECKILL_DETAIL.FK_SECKILL_ID.eq(dto.getFkSeckillId()))
                .and(SECKILL_DETAIL.ID.ne(dto.getId()))
                .exists()) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_REPEAT, "秒杀明细商品" + courseTitle + "重复");
        }

        // 组装实体类
        SeckillDetail seckillDetail = BeanUtil.copyProperties(dto, SeckillDetail.class);
        seckillDetail.setCourseTitle(courseTitle);
        seckillDetail.setCourseCover(course.getCover());
        seckillDetail.setCoursePrice(course.getPrice());
        seckillDetail.setUpdated(LocalDateTime.now());
        // update seckill_detail set fk_seckill_id = ?, fk_course_id = ?, course_title = ?, course_cover = ?, course_price = ?, sk_price = ?, sk_count = ?, info = ?, updated = ? where id = ?
        if (!UpdateChain.of(seckillDetail)
                .where(SECKILL_DETAIL.ID.eq(dto.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {
        // 检查秒杀明细是否存在
        this.existsById(id);

        // delete from seckill_detail where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查秒杀明细是否存在
        // select count(*) from seckill_detail where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(SECKILL_DETAIL.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_NOT_FOUND, "至少一个秒杀明细数据不存在");
        }

        // delete from seckill_detail where id in (?, ?, ?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查秒杀明细是否存在，如果不存在则直接抛出异常
     *
     * @param id 秒杀明细主键
     */
    private void existsById(Long id) {
        // select count(*) from seckill_detail where id = ?
        if (!QueryChain.of(mapper)
                .where(SECKILL_DETAIL.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_NOT_FOUND, id + "号秒杀明细数据不存在");
        }
    }


}
