package com.mdkj.job;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.mdkj.entity.Course;
import com.mdkj.entity.Seckill;
import com.mdkj.entity.SeckillDetail;
import com.mdkj.exception.ServiceException;
import com.mdkj.feign.CourseFeign;
import com.mdkj.mapper.SeckillMapper;
import com.mdkj.component.SeckillStockService;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.Result;
import com.mdkj.util.ResultCode;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import java.util.Date;

import static com.mdkj.entity.table.SeckillTableDef.SECKILL;
import static com.mybatisflex.core.query.QueryMethods.curDate;
import static com.mybatisflex.core.query.QueryMethods.date;

/** @author 周航宇 */
@Slf4j
@Component
public class SeckillJob {

    @Resource
	private MyRedis myRedis;
	@Resource
	private SeckillMapper seckillMapper;
    @Resource
	private CourseFeign courseFeign;
	@Resource
	private SeckillStockService seckillStockService;
	
	@XxlJob("initSeckill")
	public void initSeckill() {
	    log.info("秒杀活动预热开始");
	    XxlJobHelper.log("秒杀活动预热开始");
	
	    // 查询当天的所有秒杀活动
	    List<Seckill> todaySeckills = QueryChain.of(seckillMapper)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .orderBy(SECKILL.START_TIME.asc())
	            .withRelations()
	            .list();
	    // 判断是否查询到秒杀活动
	    if (CollUtil.isEmpty(todaySeckills)) {
	        XxlJobHelper.log("今日暂无秒杀活动，缓存预热跳过");
	        XxlJobHelper.handleSuccess("秒杀活动预热完成（无数据）");
	        return;
	    }
	
	    // 查询秒杀商品ID列表（便于后续批量查询）
	    List<Long> courseIds = new ArrayList<>();
	    todaySeckills.forEach(seckill -> {
	        // 缓存活动状态，避免 kill 热路径每次都查库
	        seckillStockService.cacheStatus(seckill.getId(), seckill.getStatus());
	        List<SeckillDetail> seckillDetails = seckill.getSeckillDetails();
	        if (ObjectUtil.isNotEmpty(seckillDetails)) {
	            seckillDetails.forEach(seckillDetail -> {
	                Long fkCourseId = seckillDetail.getFkCourseId();
	                Integer skCount = seckillDetail.getSkCount();
	                // 缓存每个商品的库存（12小时过期，07点开始缓存，18点活动结束）
	                myRedis.setEx(
	                        com.mdkj.util.SeckillRedisKeys.stock(seckill.getId(), fkCourseId),
	                        skCount.toString(),
	                        ML.Seckill.STOCK_CACHE_HOURS,
	                        TimeUnit.HOURS);
	                // 缓存商品标题/封面/价格快照，kill 热路径与下单消息可直接读取，不必再查库/查远程服务
	                seckillStockService.cacheDetail(seckill.getId(), fkCourseId,
	                        seckillDetail.getCourseTitle(), seckillDetail.getCourseCover(),
	                        seckillDetail.getCoursePrice(), seckillDetail.getSkPrice());
	                // 将商品信息加入List中
	                courseIds.add(fkCourseId);
	            });
	        }
	        log.info("秒杀活动 {} 查询到商品 {} 个", seckill.getId(), seckillDetails.size());
	        XxlJobHelper.log("秒杀活动 {} 查询到商品 {} 个", seckill.getId(), seckillDetails.size());
	    });
	
	    // 预热秒杀商品信息
	    courseIds.forEach(courseId -> {
	        // 远程调用 - 查询课程数据
	        Result<Course> courseResult = courseFeign.select(courseId);
	        if (ObjectUtil.isNull(courseResult)) {
	            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
	        }
	        Course course = courseResult.getData();
	        if (ObjectUtil.isNull(course)) {
	            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, courseId + "号课程数据不存在");
	        }
	        // 预热商品信息（12小时过期，07点开始缓存，18点活动结束）
	        myRedis.setEx(ML.Redis.SECKILL_COURSE_INFO_PREFIX + courseId, JSON.toJSONString(course), 12, TimeUnit.HOURS);
	        log.info("秒杀活动商品 {} 预热完成", courseId);
	        XxlJobHelper.log("秒杀活动商品 {} 预热完成", courseId);
	    });
	
	    log.info("秒杀活动预热结束，共预热 {} 个商品", courseIds.size());
	    XxlJobHelper.handleSuccess("秒杀活动预热完成（共预热 " + courseIds.size() + " 个商品）");
	}
	
	@XxlJob("startMorningSeckill")
	public void startMorningSeckill() {
	    updateStatusByTitle("上午场", ML.Seckill.STARTED, "开启");
	    XxlJobHelper.handleSuccess("上午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopMorningSeckill")
	public void stopMorningSeckill() {
	    updateStatusByTitle("上午场", ML.Seckill.ENDED, "关闭");
	    XxlJobHelper.handleSuccess("上午场的秒杀活动关闭成功");
	}
	
	@XxlJob("startNoonSeckill")
	public void startNoonSeckill() {
	    updateStatusByTitle("中午场", ML.Seckill.STARTED, "开启");
	    XxlJobHelper.handleSuccess("中午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopNoonSeckill")
	public void stopNoonSeckill() {
	    updateStatusByTitle("中午场", ML.Seckill.ENDED, "关闭");
	    XxlJobHelper.handleSuccess("中午场的秒杀活动关闭成功");
	}
	
	@XxlJob("startAfterNoonSeckill")
	public void startAfterNoonSeckill() {
	    updateStatusByTitle("下午场", ML.Seckill.STARTED, "开启");
	    XxlJobHelper.handleSuccess("下午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopAfterNoonSeckill")
	public void stopAfterNoonSeckill() {
	    updateStatusByTitle("下午场", ML.Seckill.ENDED, "关闭");
	    XxlJobHelper.handleSuccess("下午场的秒杀活动关闭成功");
	}

	/**
	 * 按场次标题修改当天秒杀活动状态，并立即刷新 Redis 状态缓存，
	 * 避免状态刚翻转的瞬间大量 kill 请求穿透到短 TTL 缓存过期前的窗口打到数据库。
	 */
	private void updateStatusByTitle(String title, Integer status, String actionLabel) {
	    log.info("准备{}今日{}的秒杀活动", actionLabel, title);
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, status)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq(title))
	            .update();

	    List<Long> ids = QueryChain.of(seckillMapper)
	            .select(SECKILL.ID)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq(title))
	            .listAs(Long.class);
	    ids.forEach(id -> seckillStockService.cacheStatus(id, status));

	    log.info("{}的秒杀活动已{}", title, actionLabel);
	}
}
