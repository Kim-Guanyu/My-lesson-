package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.dto.BannerInsertDTO;
import com.mdkj.dto.BannerPageDTO;
import com.mdkj.dto.BannerUpdateDTO;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.ML;
import com.mdkj.util.MinioUtil;
import com.mdkj.util.MyRedis;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Banner;
import com.mdkj.mapper.BannerMapper;
import com.mdkj.service.BannerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mdkj.entity.table.BannerTableDef.BANNER;

/**
 * 横幅表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner>  implements BannerService{

    @Resource
    private MyRedis myRedis;

    @Override
    public boolean insert(BannerInsertDTO dto) {
        // 组装实体类
        Banner banner = BeanUtil.copyProperties(dto, Banner.class);
        banner.setUrl(ML.Banner.DEFAULT_BANNER);
        banner.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        banner.setCreated(LocalDateTime.now());
        banner.setUpdated(LocalDateTime.now());
        // insert into banner (url, info, idx, created, updated) values (?, ?, ?, ?, ?)
        if (mapper.insert(banner) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        // 清除缓存
        myRedis.deleteByPrefix(ML.Redis.TOP_BANNER_KEY_PREFIX);
        return true;
    }

    @Override
    public Banner select(Long id) {
        // select * from banner where id = ?
        Banner banner = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(banner)) {
            throw new ServiceException(ResultCode.BANNER_NOT_FOUND, id + "号横幅数据不存在");
        }
        return banner;
    }

    @Override
    public PageVO<Banner> page(BannerPageDTO dto) {
        QueryChain<Banner> queryChain = QueryChain.of(mapper)
                .orderBy(BANNER.IDX.asc(), BANNER.ID.desc());

        // DB分页并转为VO
        Page<Banner> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Banner> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(BannerUpdateDTO dto) {
        Long id = dto.getId();

        // 检查横幅是否存在
        this.existsById(id);

        // 组装实体类
        Banner banner = BeanUtil.copyProperties(dto, Banner.class);
        banner.setUpdated(LocalDateTime.now());

        // update banner set url = ?, info = ?, idx = ?, updated = ? where id = ?
        if (!UpdateChain.of(banner)
                .where(BANNER.ID.eq(id))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }

        // 清除缓存
        myRedis.deleteByPrefix(ML.Redis.TOP_BANNER_KEY_PREFIX);
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查横幅是否存在
        this.existsById(id);

        // delete from banner where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        // 清除缓存
        myRedis.deleteByPrefix(ML.Redis.TOP_BANNER_KEY_PREFIX);
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查横幅是否存在
        // select count(*) from banner where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(BANNER.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.BANNER_NOT_FOUND, "至少一个横幅数据不存在");
        }

        // delete from banner where id in (?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }

        // 清除缓存
        myRedis.deleteByPrefix(ML.Redis.TOP_BANNER_KEY_PREFIX);
        return true;
    }

    /**
     * 按主键检查横幅是否存在，如果不存在则直接抛出异常
     *
     * @param id 横幅主键
     */
    private void existsById(Long id) {
        // select count(*) from banner where id = ?
        if (!QueryChain.of(mapper)
                .where(BANNER.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.BANNER_NOT_FOUND, id + "号横幅数据不存在");
        }
    }

    @Override
    public List<Banner> top(Long n) {

        String redisKey = ML.Redis.TOP_BANNER_KEY_PREFIX + n;

        // 若缓存命中，则直接返回
        if (myRedis.exists(redisKey)) {
            return JSONUtil.toList(myRedis.get(redisKey), Banner.class);
        }

        // select * from banner order by idx asc, id desc limit ?
        List<Banner> result = QueryChain.of(mapper)
                .orderBy(BANNER.IDX.asc(), BANNER.ID.desc())
                .limit(n)
                .list();

        // 将查询结果存入 redis 缓存
        myRedis.setEx(redisKey, JSONUtil.toJsonStr(result), 3, TimeUnit.HOURS);

        // 返回查询结果
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadBanner(MultipartFile newFile, Long id) {

        // 按主键查询记录
        // select * from banner where id = ?
        Banner banner = mapper.selectOneById(id);
        if (ObjectUtil.isNull(banner)) {
            throw new ServiceException(ResultCode.BANNER_NOT_FOUND, id + "号横幅数据不存在");
        }

        // 备份旧文件名
        String oldFileName = banner.getUrl();

        // 生成新文件名
        String newFileName = MinioUtil.randomFilename(newFile);

        // DB更新文件名
        banner.setUrl(newFileName);
        banner.setUpdated(LocalDateTime.now());
        if (mapper.update(banner) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库更新轮播图片名失败");
        }

        try {
            // MinIO删除旧文件（默认文件不删除）
            if (!ML.Banner.DEFAULT_BANNER.equals(oldFileName)) {
                MinioUtil.delete(oldFileName, ML.MinIO.BANNER_DIR, ML.MinIO.BUCKET_NAME);
            }

            // MinIO上传新文件
            MinioUtil.upload(newFile, newFileName, ML.MinIO.BANNER_DIR, ML.MinIO.BUCKET_NAME);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "MinIO操作失败：" + e.getMessage());
        }

        // 返回新文件名
        return newFileName;
    }




}
