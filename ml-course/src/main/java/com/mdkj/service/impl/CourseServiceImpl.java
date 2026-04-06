package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mdkj.dao.CourseRepository;
import com.mdkj.dto.CourseInsertDTO;
import com.mdkj.dto.CoursePageDTO;
import com.mdkj.dto.CourseSimpleListVO;
import com.mdkj.dto.CourseUpdateDTO;
import com.mdkj.es.CourseDoc;
import com.mdkj.exception.ServiceException;
import com.mdkj.mapper.EpisodeMapper;
import com.mdkj.mapper.SeasonMapper;
import com.mdkj.util.ML;
import com.mdkj.util.MinioUtil;
import com.mdkj.util.MyRedis;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Course;
import com.mdkj.mapper.CourseMapper;
import com.mdkj.service.CourseService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.CourseTableDef.COURSE;
import static com.mdkj.entity.table.EpisodeTableDef.EPISODE;
import static com.mdkj.entity.table.SeasonTableDef.SEASON;

/**
 * 课程表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>  implements CourseService{

    @Resource
    private MyRedis redis;
    @Resource
    private SeasonMapper seasonMapper;
    @Resource
    private EpisodeMapper episodeMapper;

    @Resource
    private MinioUtil minioUtil;

    @Resource
    private CourseRepository courseRepository;

    @Override
    public boolean insert(CourseInsertDTO dto) {
        String title = dto.getTitle();

        // 标题查重
        // select count(*) from course where title = ?
        if (QueryChain.of(mapper)
                .where(COURSE.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Course course = BeanUtil.copyProperties(dto, Course.class);
        course.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        course.setSummary(ML.Course.DEFAULT_SUMMARY);
        course.setCover(ML.Course.DEFAULT_COVER);
        course.setCreated(LocalDateTime.now());
        course.setUpdated(LocalDateTime.now());

        // insert into course (title, author, fk_category_id, info, summary, cover, price, idx, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(course) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Course select(Long id) {
        // 从Redis缓存中获取课程信息
        String courseFromRedis = redis.get(ML.Redis.SECKILL_COURSE_INFO_PREFIX + id);
        if (ObjectUtil.isNotNull(courseFromRedis)) {
            return JSON.parseObject(courseFromRedis, Course.class);
        }

        // 指定联查字段
        RelationManager.addQueryRelations("category", "seasons", "episodes");
        // select * from course where id = ?
        Course course = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, id + "号课程数据不存在");
        }
        return course;
    }


    @Override
    public List<CourseSimpleListVO> simpleList() {
        // select id, title, price from course order by idx asc, id desc
        return QueryChain.of(mapper)
                .orderBy(COURSE.IDX.asc(), COURSE.ID.desc())
                .withRelations()
                .listAs(CourseSimpleListVO.class);
    }

    @Override
    public PageVO<Course> page(CoursePageDTO dto) {

        // 指定联查字段
        RelationManager.addQueryRelations("category", "seasons", "episodes");

        // select * from course order by fk_category_id asc, idx asc, id desc
        QueryChain<Course> queryChain = QueryChain.of(mapper)
                .orderBy(COURSE.FK_CATEGORY_ID.asc(), COURSE.IDX.asc(), COURSE.ID.desc());

        // title条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(COURSE.TITLE.like(title));
        }

        // categoryId条件
        Long categoryId = dto.getFkCategoryId();
        if (ObjectUtil.isNotNull(categoryId)) {
            queryChain.where(COURSE.FK_CATEGORY_ID.eq(categoryId));
        }

        // DB分页并转为VO
        Page<Course> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Course> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(CourseUpdateDTO dto) {
        String title = dto.getTitle();
        Long id = dto.getId();

        // 检查课程是否存在
        this.existsById(id);

        // 标题查重
        // select count(1) from course where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(COURSE.TITLE.eq(dto.getTitle()))
                .and(COURSE.ID.ne(dto.getId()))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Course course = BeanUtil.copyProperties(dto, Course.class);
        course.setUpdated(LocalDateTime.now());
        // update course set title = ?, author = ?, fk_category_id = ?, info = ?, summary = ?, cover = ?, price = ?, idx = ?, updated = ? where id = ?
        if (!UpdateChain.of(course)
                .where(COURSE.ID.eq(course.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id) {

        // 检查课程是否存在
        this.existsById(id);

        // 通过课程主键查询全部季次的ID列表
        // select id from season where fk_course_id = ?
        List<Long> seasonIds = QueryChain.of(seasonMapper)
                .select(SEASON.ID)
                .where(SEASON.FK_COURSE_ID.eq(id))
                .objListAs(Long.class);

        // 存在季记录时，批量删除季
        this.clearSeasonAndEpisode(seasonIds);

        // 删除课程
        // delete from course where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查课程是否存在
        // select count(*) from course where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(COURSE.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, "至少一个课程数据不存在");
        }

        // 通过课程主键列表批量查询全部季次的ID列表
        // select id from season where fk_course_id in (?)
        List<Long> seasonIds = QueryChain.of(seasonMapper)
                .select(SEASON.ID)
                .where(SEASON.FK_COURSE_ID.in(ids))
                .objListAs(Long.class);

        // 存在季记录时，批量删除季
        this.clearSeasonAndEpisode(seasonIds);

        // 批量删除课程
        // delete from course where id in (?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查课程是否存在，如果不存在则直接抛出异常
     *
     * @param id 课程主键
     */
    private void existsById(Long id) {
        // select count(*) from course where id = ?
        if (!QueryChain.of(mapper)
                .where(COURSE.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, id + "号课程数据不存在");
        }
    }

    /**
     * 根据季次ID列表，清空全部季次记录及每个季次中的集次记录
     *
     * @param seasonIds 季次主键列表
     */
    private void clearSeasonAndEpisode(List<Long> seasonIds) {

        // 存在季次时，批量删除季次
        if (ObjectUtil.isNotEmpty(seasonIds)) {

            // 通过季次主键列表查询全部集次的ID列表
            // select id from episode where fk_season_id in (?)
            List<Long> episodeIds = QueryChain.of(episodeMapper)
                    .select(EPISODE.ID)
                    .where(EPISODE.FK_SEASON_ID.in(seasonIds))
                    .objListAs(Long.class);

            // 存在集次时，批量删除集次
            if (ObjectUtil.isNotEmpty(episodeIds)) {
                if (episodeMapper.deleteBatchByIds(episodeIds) <= 0) {
                    throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除集次数据失败");
                }
            }

            // 批量删除季次
            if (seasonMapper.deleteBatchByIds(seasonIds) <= 0) {
                throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除季次数据失败");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadCover(MultipartFile newFile, Long id) {

        // 按主键查询记录
        // select * from course where id = ?
        Course course = mapper.selectOneById(id);
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, id + "号课程数据不存在");
        }

        // 备份旧文件名
        String oldFileName = course.getCover();

        // 生成新文件名
        String newFileName = MinioUtil.randomFilename(newFile);

        // DB更新文件名
        course.setCover(newFileName);
        course.setUpdated(LocalDateTime.now());
        if (mapper.update(course) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库更新封面图片名失败");
        }

        try {
            // MinIO删除旧文件（默认文件不删除）
            if (!ML.Course.DEFAULT_COVER.equals(oldFileName)) {
                MinioUtil.delete(oldFileName, ML.MinIO.COURSE_COVER_DIR, ML.MinIO.BUCKET_NAME);
            }

            // MinIO上传新文件
            MinioUtil.upload(newFile, newFileName, ML.MinIO.COURSE_COVER_DIR, ML.MinIO.BUCKET_NAME);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "MinIO操作失败：" + e.getMessage());
        }

        // 返回新文件名
        return newFileName;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadSummary(MultipartFile newFile, Long id) {

        // 按主键查询记录
        // select * from course where id = ?
        Course course = mapper.selectOneById(id);
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, id + "号课程数据不存在");
        }

        // 备份旧文件名
        String oldFileName = course.getSummary();

        // 生成新文件名
        String newFileName = MinioUtil.randomFilename(newFile);

        // DB更新文件名
        course.setSummary(newFileName);
        course.setUpdated(LocalDateTime.now());
        if (mapper.update(course) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库更新摘要图片名失败");
        }

        try {
            // MinIO删除旧文件（默认文件不删除）
            if (!ML.Course.DEFAULT_SUMMARY.equals(oldFileName)) {
                MinioUtil.delete(oldFileName, ML.MinIO.COURSE_SUMMARY_DIR, ML.MinIO.BUCKET_NAME);
            }

            // MinIO上传新文件
            MinioUtil.upload(newFile, newFileName, ML.MinIO.COURSE_SUMMARY_DIR, ML.MinIO.BUCKET_NAME);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "MinIO操作失败：" + e.getMessage());
        }

        // 返回文件名
        return newFileName;
    }

    @Override
    public PageVO<CourseDoc> search(CoursePageDTO dto) {
        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        String keyword = dto.getKeyword();
        org.springframework.data.domain.Page<CourseDoc> esPage;

        // 默认值
        pageNum = ObjectUtil.isNotNull(pageNum) ? pageNum : 0;
        pageSize = ObjectUtil.isNotNull(pageSize) ? pageSize : 10;

        // 参数处理: 对pageNum进行最小边界保护，规避ES报错，注意ES的分页是从0开始的
        pageNum = pageNum - 1;
        if (pageNum < 0) pageNum = 0;
        if (pageSize < 0) pageSize = 0;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        // 关键字为空时分页全查，不为空时候按关键字搜索
        if (StrUtil.isEmpty(keyword)) {
            esPage = courseRepository.findAll(pageable);
        } else {
            esPage = courseRepository.searchByTitleOrAuthorOrderByIdx(keyword, keyword, pageable);
        }

        // 组装 PageVO 并返回
        PageVO<CourseDoc> result = new PageVO<>();
        result.setPageNum(pageNum + 1L);
        result.setPageSize(pageSize);
        result.setTotalRow(esPage.getTotalElements());
        result.setTotalPage(esPage.getTotalPages());
        result.setRecords(esPage.getContent());
        return result;
    }


}
