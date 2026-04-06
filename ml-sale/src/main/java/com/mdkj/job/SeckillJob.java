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
	        List<SeckillDetail> seckillDetails = seckill.getSeckillDetails();
	        if (ObjectUtil.isNotEmpty(seckillDetails)) {
	            seckillDetails.forEach(seckillDetail -> {
	                Long fkCourseId = seckillDetail.getFkCourseId();
	                Integer skCount = seckillDetail.getSkCount();
	                // 缓存每个商品的库存（12小时过期，07点开始缓存，18点活动结束）
	                myRedis.setEx(ML.Redis.SECKILL_COURSE_COUNT_PREFIX + fkCourseId, skCount.toString(), 12, TimeUnit.HOURS);
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
	    log.info("准备开启今日上午场的秒杀活动");
	
	    // 修改当天的上午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.STARTED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("上午场"))
	            .update();
	
	    log.info("上午场的秒杀活动已开启");
	    XxlJobHelper.handleSuccess("上午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopMorningSeckill")
	public void stopMorningSeckill() {
	    log.info("准备关闭今日上午场的秒杀活动");
	
	    // 修改当天的上午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.ENDED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("上午场"))
	            .update();
	
	    log.info("上午场的秒杀活动已关闭");
	    XxlJobHelper.handleSuccess("上午场的秒杀活动关闭成功");
	}
	
	@XxlJob("startNoonSeckill")
	public void startNoonSeckill() {
	    log.info("准备开启今日中午场的秒杀活动");
	
	    // 修改当天的中午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.STARTED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("中午场"))
	            .update();
	
	    log.info("中午场的秒杀活动已开启");
	    XxlJobHelper.handleSuccess("中午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopNoonSeckill")
	public void stopNoonSeckill() {
	    log.info("准备关闭今日中午场的秒杀活动");
	
	    // 修改当天的中午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.ENDED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("中午场"))
	            .update();
	
	    log.info("中午场的秒杀活动已关闭");
	    XxlJobHelper.handleSuccess("中午场的秒杀活动关闭成功");
	}
	
	@XxlJob("startAfterNoonSeckill")
	public void startAfterNoonSeckill() {
	    log.info("准备开启今日下午场的秒杀活动");
	
	    // 修改当天的下午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.STARTED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("下午场"))
	            .update();
	
	    log.info("下午场的秒杀活动已开启");
	    XxlJobHelper.handleSuccess("下午场的秒杀活动开启成功");
	}
	
	@XxlJob("stopAfterNoonSeckill")
	public void stopAfterNoonSeckill() {
	    log.info("准备关闭今日下午场的秒杀活动");
	
	    // 修改当天的下午场的秒杀活动状态
	    UpdateChain.of(seckillMapper)
	            .set(SECKILL.STATUS, ML.Seckill.ENDED)
	            .where(date(SECKILL.START_TIME).eq(curDate()))
	            .and(SECKILL.TITLE.eq("下午场"))
	            .update();
	
	    log.info("下午场的秒杀活动已关闭");
	    XxlJobHelper.handleSuccess("下午场的秒杀活动关闭成功");
	}
}
