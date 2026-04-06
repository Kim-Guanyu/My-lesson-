package com.mdkj.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Getter
@Component
public class MyRedis {

    private final StringRedisTemplate stringRedisTemplate;
    private final ValueOperations<String, String> stringOps;
    private final HashOperations<String, Object, Object> hashOps;
    private final ListOperations<String, String> listOps;
    private final SetOperations<String, String> setOps;
    private final ZSetOperations<String, String> zsetOps;
    private final GeoOperations<String, String> geoOps;

    @Autowired
    public MyRedis(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.stringOps = stringRedisTemplate.opsForValue();
        this.hashOps = stringRedisTemplate.opsForHash();
        this.listOps = stringRedisTemplate.opsForList();
        this.setOps = stringRedisTemplate.opsForSet();
        this.zsetOps = stringRedisTemplate.opsForZSet();
        this.geoOps = stringRedisTemplate.opsForGeo();
    }

    /**
     * COMMON: 按缓存名单删一条缓存记录
     *
     * @param key 缓存名
     */
    public void del(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * COMMON: 按缓存名的集合批删多条缓存记录
     *
     * @param keys 缓存名的集合
     */
    public void del(Set<String> keys) {
        stringRedisTemplate.delete(keys);
    }

    /**
     * COMMON: 按缓存名查询该缓存记录是否存在
     *
     * @param key 缓存名
     * @return true表示缓存记录存在，false表示缓存记录不存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * COMMON: 为指定的缓存设置过期时间
     *
     * @param key     缓存名
     * @param timeout 缓存过期时间
     * @param unit    缓存过期时间单位，如 TimeUnit.MILLISECONDS 等
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * STRING: 永久存储一条字符串类型的缓存记录
     *
     * @param key 缓存名
     * @param val 字符串类型的缓存记录
     */
    public void set(String key, String val) {
        stringOps.set(key, val);
    }

    /**
     * STRING: 永久存储一条字符串类型的缓存记录，仅在缓存名不存在时才会成功
     *
     * @param key 缓存名
     * @param val 字符串类型的缓存记录
     * @return true表示存储成功，false表示存储失败
     */
    public boolean setNx(String key, String val) {
        return Boolean.TRUE.equals(stringOps.setIfAbsent(key, val));
    }

    /**
     * STRING: 带期限存储一条字符串类型的缓存记录
     *
     * @param key     缓存名
     * @param val     字符串类型的缓存记录
     * @param timeout 缓存过期时间
     * @param unit    缓存过期时间单位，如 TimeUnit.MILLISECONDS 等
     */
    public void setEx(String key, String val, long timeout, TimeUnit unit) {
        stringOps.set(key, val, timeout, unit);
    }

    /**
     * STRING: 按缓存名查询一条字符串类型的缓存记录
     *
     * @param key 缓存名
     * @return 指定key的缓存数据
     */
    public String get(String key) {
        return stringOps.get(key);
    }

    /**
     * STRING: 按缓存名查询一条字符串类型的缓存记录的字节长度
     *
     * @param key 缓存名
     * @return 缓存记录的字节长度
     */
    public long strLen(String key) {
        Long result = stringOps.size(key);
        assert result != null;
        return result;
    }

    /**
     * STRING: 按缓存名自增一条字符串类型的缓存记录
     *
     * @param key       缓存名
     * @param increment 自增整数，负数表示自减
     * @return 自增后的结果
     */
    public long incr(String key, long increment) {
        Long result = stringOps.increment(key, increment);
        assert result != null;
        return result;
    }

    /**
     * STRING: 按缓存名自增一条字符串类型的缓存记录
     *
     * @param key       缓存名
     * @param increment 自增浮点数，负数表示自减
     * @return 自增后的结果
     */
    public double incr(String key, double increment) {
        Double result = stringOps.increment(key, increment);
        assert result != null;
        return result;
    }

    /**
     * STRING: 按缓存名的集合批删查询多条字符串类型的缓存记录
     *
     * @param keys 缓存名的集合
     * @return 多条字符串类型的缓存记录
     */
    public List<String> mGet(Set<String> keys) {
        return stringOps.multiGet(keys);
    }

    /**
     * STRING: 批删设置多条字符串类型的缓存记录
     *
     * @param map 格式为 {缓存名, 缓存记录}
     */
    public void mSet(Map<String, String> map) {
        stringOps.multiSet(map);
    }

    /**
     * HASH: 向指定hash容器添加一条属性
     *
     * @param key   hash容器名，容器自动创建与销毁
     * @param field 属性名
     * @param val   属性值
     */
    public void hSet(String key, String field, String val) {
        hashOps.put(key, field, val);
    }

    /**
     * HASH: 向指定hash容器添加一条属性，仅在容器名不存在时才会成功
     *
     * @param key   hash容器名，容器自动创建与销毁
     * @param field 属性名
     * @param val   属性值
     * @return true表示添加成功，false表示添加失败
     */
    public boolean hSetNx(String key, String field, String val) {
        return hashOps.putIfAbsent(key, field, val);
    }

    /**
     * HASH: 返回指定hash容器中是否存在指定属性
     *
     * @param key   hash容器名，容器自动创建与销毁
     * @param field 属性名
     * @return true表示属性存在，false表示属性不存在
     */
    public boolean hExists(String key, String field) {
        return hashOps.hasKey(key, field);
    }

    /**
     * HASH: 返回指定hash容器中的指定属性
     *
     * @param key   hash容器名，容器自动创建与销毁
     * @param field 属性名
     * @return 属性值
     */
    public Object hGet(String key, String field) {
        return hashOps.get(key, field);
    }

    /**
     * HASH: 返回指定hash容器中的全部属性以及对应的属性值
     *
     * @param key hash容器名，容器自动创建与销毁
     * @return 全部属性以及对应的属性值
     */
    public Map<Object, Object> hGetAll(String key) {
        return hashOps.entries(key);
    }

    /**
     * HASH: 返回指定hash容器中的全部属性集合
     *
     * @param key hash容器名，容器自动创建与销毁
     * @return 全部属性集合
     */
    public Set<Object> hKeys(String key) {
        return hashOps.keys(key);
    }

    /**
     * HASH: 返回指定hash容器中的全部属性值列表
     *
     * @param key hash容器名，容器自动创建与销毁
     * @return 全部属性值列表
     */
    public List<Object> hVals(String key) {
        return hashOps.values(key);
    }

    /**
     * HASH: 对指定hash容器中的指定属性进行自增
     *
     * @param key       hash容器名，容器自动创建与销毁
     * @param field     属性名称
     * @param increment 自增整数，负数表示自减
     * @return 自增后的结果
     */
    public long hIncrBy(String key, Object field, long increment) {
        return hashOps.increment(key, field, increment);
    }

    /**
     * HASH: 对指定hash容器中的指定属性进行自增
     *
     * @param key       hash容器名，容器自动创建与销毁
     * @param field     属性名称
     * @param increment 自增浮点数，负数表示自减
     * @return 自增后的结果
     */
    public double hIncrBy(String key, Object field, double increment) {
        return hashOps.increment(key, field, increment);
    }

    /**
     * HASH: 返回指定hash容器中的属性个数
     *
     * @param key hash容器名，容器自动创建与销毁
     * @return 属性个数
     */
    public long hLen(String key) {
        return hashOps.size(key);
    }

    /**
     * HASH: 从指定hash容器中，按指定的属性数组批量删除属性
     *
     * @param key    hash容器名，容器自动创建与销毁
     * @param fields 属性数组
     * @return 影响条目数
     */
    public long hDelete(String key, Object... fields) {
        return hashOps.delete(key, fields);
    }

    /**
     * LIST: 在指定list容器的头部插入一个元素
     *
     * @param key list容器名，容器自动创建与销毁
     * @param val 元素
     * @return 返回插入元素后的容器长度
     */
    public long lPush(String key, String val) {
        Long result = listOps.leftPush(key, val);
        assert result != null;
        return result;
    }

    /**
     * LIST: 在指定list容器的头部插入多个元素
     *
     * @param key  list容器名，容器自动创建与销毁
     * @param vals 元素数组
     * @return 返回插入元素后的容器长度
     */
    public Long lPush(String key, String... vals) {
        return listOps.leftPushAll(key, vals);
    }

    /**
     * LIST: 在指定list容器的指定支点元素前插入指定元素
     *
     * @param key   list容器名，容器自动创建与销毁
     * @param pivot 支点元素
     * @param val   元素
     * @return 返回插入元素后的容器长度
     */
    public Long lInsertBefore(String key, String pivot, String val) {
        return listOps.leftPush(key, pivot, val);
    }

    /**
     * LIST: 在指定list容器的指定支点元素后插入指定元素
     *
     * @param key   list容器名，容器自动创建与销毁
     * @param pivot 支点元素
     * @param val   元素
     * @return 返回插入元素后的容器长度
     */
    public Long lInsertAfter(String key, String pivot, String val) {
        return listOps.rightPush(key, pivot, val);
    }

    /**
     * LIST: 在指定list容器的尾部插入一个元素
     *
     * @param key list容器名，容器自动创建与销毁
     * @param val 元素
     * @return 返回插入元素后的容器长度
     */
    public Long rPush(String key, String val) {
        return listOps.rightPush(key, val);
    }

    /**
     * LIST: 在指定list容器的尾部插入多个元素
     *
     * @param key  list容器名，容器自动创建与销毁
     * @param vals 元素数组
     * @return 返回插入元素后的容器长度
     */
    public Long rPush(String key, String... vals) {
        return listOps.rightPushAll(key, vals);
    }

    /**
     * LIST: 返回指定list容器中的元素个数
     *
     * @param key list容器名，容器自动创建与销毁
     * @return list容器中的元素个数
     */
    public Long lLen(String key) {
        return listOps.size(key);
    }

    /**
     * LIST: 返回指定list容器中指定范围内的全部元素
     *
     * @param key   list容器名，容器自动创建与销毁
     * @param start 开始位置，索引从0开始
     * @param end   结束位置，-1表示最后位置
     * @return list容器中指定范围内的全部元素
     */
    public List<String> lRange(String key, long start, long end) {
        return listOps.range(key, start, end);
    }

    /**
     * LIST: 弹出指定list容器中的头部元素
     *
     * @param key list容器名，容器自动创建与销毁
     * @return 弹出的元素
     */
    public String lPop(String key) {
        return listOps.leftPop(key);
    }

    /**
     * LIST: 弹出指定list容器中的尾部元素
     *
     * @param key list容器名，容器自动创建与销毁
     * @return 弹出的元素
     */
    public String rPop(String key) {
        return listOps.rightPop(key);
    }

    /**
     * LIST: 通过索引返回list容器中的指定元素
     *
     * @param key   list容器名，容器自动创建与销毁
     * @param index 索引，从0开始
     * @return 指定索引位置上的元素
     */
    public String lIndex(String key, long index) {
        return listOps.index(key, index);
    }

    /**
     * LIST: 通过索引设置list容器中的指定元素
     *
     * @param key   list容器名，容器自动创建与销毁
     * @param index 索引，从0开始
     * @param val   新值
     */
    public void lSet(String key, long index, String val) {
        listOps.set(key, index, val);
    }

    /**
     * SET: 向指定set容器中添加多个元素
     *
     * @param key  set容器名，容器自动创建与销毁
     * @param vals 元素数组
     * @return 影响条目数
     */
    public long sAdd(String key, String... vals) {
        Long result = setOps.add(key, vals);
        assert result != null;
        return result;
    }

    /**
     * SET: 返回指定set容器中的元素个数
     *
     * @param key set容器名，容器自动创建与销毁
     * @return set容器中的元素个数
     */
    public Long sCard(String key) {
        return setOps.size(key);
    }

    /**
     * SET: 返回指定set容器中的全部元素，重量级命令
     *
     * @param key set容器名，容器自动创建与销毁
     * @return set容器中的全部元素
     */
    public Set<String> sMembers(String key) {
        return setOps.members(key);
    }

    /**
     * SET: 从指定set容器中删除多个元素
     *
     * @param key  set容器名，容器自动创建与销毁
     * @param vals 元素数组
     * @return 影响条目数
     */
    public long sRem(String key, Object... vals) {
        Long result = setOps.remove(key, vals);
        assert result != null;
        return result;
    }

    /**
     * SET: 返回指定set容器中是否包含指定元素
     *
     * @param key set容器名，容器自动创建与销毁
     * @param val 元素
     * @return true表示包含指定元素，false表示不包含指定元素
     */
    public boolean sIsMember(String key, Object val) {
        Boolean result = setOps.isMember(key, val);
        assert result != null;
        return result;
    }

    /**
     * SET: 随机返回指定set容器中指定数量的元素
     *
     * @param key   set容器名，容器自动创建与销毁
     * @param count 指定数量，负数表示结果不重复，正数表示结果可重复
     * @return 随机元素列表
     */
    public Collection<String> sRandomMember(String key, long count) {
        if (count < 0) {
            count = Math.abs(count);
            return setOps.distinctRandomMembers(key, count);
        } else if (count > 0) {
            return setOps.randomMembers(key, count);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * SET: 随机弹出指定set容器中指定数量的元素
     *
     * @param key   set容器名，容器自动创建与销毁
     * @param count 指定数量，负数表示结果不重复，正数表示结果可重复
     * @return 随机元素列表
     */
    public List<String> sPop(String key, long count) {
        return setOps.pop(key, count);
    }

    /**
     * SET: 计算两个set容器的差集并存储到目标set容器中
     *
     * @param key01 操作容器01
     * @param key02 操作容器02
     * @param key03 目标set容器
     * @return 影响条目数
     */
    public long sDiffStore(String key01, String key02, String key03) {
        Long result = setOps.differenceAndStore(key01, key02, key03);
        assert result != null;
        return result;
    }

    /**
     * SET: 计算两个set容器的交集并存储到目标set容器中
     *
     * @param key01 操作容器01
     * @param key02 操作容器02
     * @param key03 目标set容器
     * @return 影响条目数
     */
    public long sInterStore(String key01, String key02, String key03) {
        Long result = setOps.intersectAndStore(key01, key02, key03);
        assert result != null;
        return result;
    }

    /**
     * SET: 计算两个set容器的并集并存储到目标set容器中
     *
     * @param key01 操作容器01
     * @param key02 操作容器02
     * @param key03 目标set容器
     * @return 影响条目数
     */
    public long sUnionStore(String key01, String key02, String key03) {
        Long result = setOps.unionAndStore(key01, key02, key03);
        assert result != null;
        return result;
    }

    /**
     * ZSET: 向指定zset容器中添加元素
     *
     * @param key   zset容器名，容器自动创建与销毁
     * @param val   元素
     * @param score 元素得分
     * @return 影响条目数
     */
    public boolean zAdd(String key, String val, double score) {
        Boolean result = zsetOps.add(key, val, score);
        assert result != null;
        return result;
    }

    /**
     * ZSET: 返回指定zset容器中的元素个数
     *
     * @param key zset容器名，容器自动创建与销毁
     * @return zset容器中的元素个数
     */
    public Long zCard(String key) {
        return zsetOps.zCard(key);
    }

    /**
     * ZSET: 返回指定zset容器中指定分数范围内的元素个数
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数范围内的元素个数
     */
    public Long zCount(String key, double min, double max) {
        return zsetOps.count(key, min, max);
    }

    /**
     * ZSET: 返回指定zset容器中指定元素的分数
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param val 元素
     * @return 指定元素的分数
     */
    public Double zScore(String key, Object val) {
        return zsetOps.score(key, val);
    }

    /**
     * ZSET: 升序返回指定zset容器中指定索引范围内的元素
     *
     * @param key   zset容器名，容器自动创建与销毁
     * @param start 索引起始位置，从0开始
     * @param end   索引结束位置，-1表示最后
     * @return 指定索引范围内的元素
     */
    public Set<String> zRange(String key, long start, long end) {
        return zsetOps.range(key, start, end);
    }

    /**
     * ZSET: 降序返回指定zset容器中指定索引范围内的元素
     *
     * @param key   zset容器名，容器自动创建与销毁
     * @param start 索引起始位置，从0开始
     * @param end   索引结束位置，-1表示最后
     * @return 指定索引范围内的元素
     */
    public Set<String> zRevRange(String key, long start, long end) {
        return zsetOps.reverseRange(key, start, end);
    }

    /**
     * ZSET: 带分数升序返回指定zset容器中指定索引范围内的元素
     *
     * @param key   zset容器名，容器自动创建与销毁
     * @param start 索引起始位置，从0开始
     * @param end   索引结束位置，-1表示最后
     * @return 指定索引范围内的元素
     */
    public Set<ZSetOperations.TypedTuple<String>> zRangeWithScores(
            String key, long start, long end) {
        return zsetOps.rangeWithScores(key, start, end);
    }

    /**
     * ZSET: 带分数降序返回指定zset容器中指定索引范围内的元素
     *
     * @param key   zset容器名，容器自动创建与销毁
     * @param start 索引起始位置，从0开始
     * @param end   索引结束位置，-1表示最后
     * @return 指定索引范围内的元素
     */
    public Set<ZSetOperations.TypedTuple<String>> zRevRangeWithScores(
            String key, long start, long end) {
        return zsetOps.reverseRangeWithScores(key, start, end);
    }

    /**
     * ZSET: 升序返回指定zset容器中指定分数范围内的元素
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数范围内的元素
     */
    public Set<String> zRangeByScore(String key, double min, double max) {
        return zsetOps.rangeByScore(key, min, max);
    }

    /**
     * ZSET: 降序返回指定zset容器中指定分数范围内的元素
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数范围内的元素
     */
    public Set<String> zRevRangeByScore(String key, double min, double max) {
        return zsetOps.reverseRangeByScore(key, min, max);
    }

    /**
     * ZSET: 带分数升序返回指定zset容器中指定分数范围内的元素
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数范围内的元素
     */
    public Set<ZSetOperations.TypedTuple<String>> zRangeByScoreWithScores(
            String key, double min, double max) {
        return zsetOps.rangeByScoreWithScores(key, min, max);
    }

    /**
     * ZSET: 带分数降序返回指定zset容器中指定分数范围内的元素
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数范围内的元素
     */
    public Set<ZSetOperations.TypedTuple<String>> zRevRangeByScoreWithScores(
            String key, double min, double max) {
        return zsetOps.reverseRangeByScoreWithScores(key, min, max);
    }

    /**
     * ZSET: 返回指定zset容器中指定元素的升序排名
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param val 元素
     * @return 指定元素的升序排名，从0开始
     */
    public Long zRank(String key, Object val) {
        return zsetOps.rank(key, val);
    }

    /**
     * ZSET: 返回指定zset容器中指定元素的降序排名
     *
     * @param key zset容器名，容器自动创建与销毁
     * @param val 元素
     * @return 指定元素的降序排名，从0开始
     */
    public Long zRevRank(String key, Object val) {
        return zsetOps.reverseRank(key, val);
    }

    /**
     * ZSET: 从指定zset容器中批量删除多个元素
     *
     * @param key  zset容器名，容器自动创建与销毁
     * @param vals 元素数组
     * @return 影响条目数
     */
    public Long zRem(String key, Object... vals) {
        return zsetOps.remove(key, vals);
    }

    /**
     * ZSET: 对指定zset容器中的指定元素自增指定分数
     *
     * @param key       zset容器名，容器自动创建与销毁
     * @param val       元素
     * @param increment 自增浮点型分数，负数表示自减
     * @return 自增后的分数
     */
    public double zIncrBy(String key, String val, double increment) {
        Double result = zsetOps.incrementScore(key, val, increment);
        assert result != null;
        return result;
    }

    /**
     * PIPELINE：客户端批量打包命令并分批发送给服务端，服务端解包命令并非原子性执行
     *
     * @param task pipeline任务，返回值必须为null
     * @return 全部命令的执行结果的结果集合
     */
    public List<Object> pipeline(RedisCallback<Long> task) {
        return stringRedisTemplate.executePipelined(task);
    }

    /**
     * GEO：向指定GEO容器中添加指定经纬度的元素
     *
     * @param key       GEO容器名，容器自动创建与销毁
     * @param longitude 元素经度
     * @param latitude  元素纬度
     * @param member    元素名称
     */
    public void geoAdd(String key, double longitude, double latitude, String member) {
        geoOps.add(key, new Point(longitude, latitude), member);
    }

    /**
     * GEO：从指定GEO容器中批量获取指定元素集合的经纬度坐标集合
     *
     * @param key     GEO容器名，容器自动创建与销毁
     * @param members 元素集合
     * @return 经纬度坐标集合
     */
    public List<Point> geoPos(String key, String... members) {
        return geoOps.position(key, members);
    }

    /**
     * GEO：计算两个元素之间的直线距离
     *
     * @param key     GEO容器名，容器自动创建与销毁
     * @param member1 第一个元素
     * @param member2 第二个元素
     * @param unit    距离单位
     * @return 两个元素之间的直线距离
     */
    public double geoDist(String key, String member1, String member2, Metrics unit) {
        Distance distance = geoOps.distance(key, member1, member2, unit);
        if (distance == null) throw new RuntimeException("距离为空");
        return distance.getValue();
    }

    /**
     * GEO：在指定GEO容器中，获取以指定经纬度为中心，以指定距离为半径的范围内的全部元素
     *
     * @param key       GEO容器名，容器自动创建与销毁
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    距离
     * @param unit      距离单位
     * @param limit     限制最多查找多少条记录，数值小于1时，视为1
     * @return 全部元素集合，以距离升序，结果集中携带经纬度和距离
     */
    public List<Map<String, Object>> geoRadius(String key, double longitude, double latitude,
                                               double radius, Metrics unit, long limit) {

        // limit数值小于1时，视为1
        if (limit < 1) limit = 1;

        // GEO范围配置参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs() // 创建配置实例
                .includeCoordinates() // 配置结果集中携带经纬度
                .includeDistance() // 配置结果集中携带距离
                .limit(limit) // 配置仅展示limit个元素
                .sortAscending();// 配置将结果集按距离升序

        // GEO范围扫描
        Point point = new Point(longitude, latitude);
        Distance distance = new Distance(radius, unit);
        Circle circle = new Circle(point, distance);
        GeoResults<RedisGeoCommands.GeoLocation<String>> members = geoOps.radius(key, circle, args);
        if (members == null) throw new RuntimeException("结果集为空");

        // 处理结果集
        List<Map<String, Object>> result = new ArrayList<>();
        members.forEach(member -> {
            Map<String, Object> map = new HashMap<>(3);
            map.put("name", member.getContent().getName());
            map.put("longitude", member.getContent().getPoint().getX());
            map.put("latitude", member.getContent().getPoint().getY());
            map.put("distance", member.getDistance().getValue());
            result.add(map);
        });
        return result;
    }

    /**
     * GEO：在指定GEO容器中，获取以指定元素为中心，以指定距离为半径的范围内的全部元素
     *
     * @param key    GEO容器名，容器自动创建与销毁
     * @param member 中心元素
     * @param radius 距离
     * @param unit   距离单位
     * @param limit  限制最多查找多少条记录，数值小于1时，视为1
     * @return 全部元素集合，以距离升序，结果集中携带经纬度和距离
     */
    public List<Map<String, Object>> geoRadiusByMember(String key, String member,
                                                       double radius, Metrics unit, long limit) {

        // limit数值小于1时，视为1
        if (limit < 1) limit = 1;

        // GEO范围配置参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs() // 创建配置实例
                .includeCoordinates() // 配置结果集中携带经纬度
                .includeDistance() // 配置结果集中携带距离
                .limit(limit) // 配置仅展示limit个元素
                .sortAscending();// 配置将结果集按距离升序

        // GEO范围扫描
        Distance distance = new Distance(radius, unit);
        GeoResults<RedisGeoCommands.GeoLocation<String>> members = geoOps.radius(key, member, distance, args);
        if (members == null) throw new RuntimeException("结果集为空");

        // 处理结果集
        List<Map<String, Object>> result = new ArrayList<>();
        members.forEach(e -> {
            Map<String, Object> map = new HashMap<>(3);
            map.put("name", e.getContent().getName());
            map.put("longitude", e.getContent().getPoint().getX());
            map.put("latitude", e.getContent().getPoint().getY());
            map.put("distance", e.getDistance().getValue());
            result.add(map);
        });
        return result;
    }

    /**
     * BITMAP：获取指定BitMap中指定位置的二进制值
     *
     * @param key    BitMap名称
     * @param offset 指定位置
     * @return 返回 "0" 或者 "1"
     */
    public String getBit(String key, long offset) {
        if (key == null) {
            throw new RuntimeException("key为空");
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(key, offset)) ? "1" : "0";
    }

    /**
     * BITMAP：获取指定Bitmap字符串表现形式
     *
     * @param key Bitmap名称
     * @return Bitmap字符串
     */
    public String bitmap(String key) {
        if (key == null) {
            throw new RuntimeException("key为空");
        }
        StringBuilder result = new StringBuilder();
        for (long i = 0, j = this.strLen(key) * 8; i < j; i++) {
            result.append(this.getBit(key, i));
        }
        return result.toString();
    }

    /**
     * BITMAP：获取指定Bitmap字符串中1的个数
     *
     * @param key Bitmap名称
     * @return Bitmap中1的个数
     */
    public long bitCount(String key) {
        if (key == null) {
            throw new RuntimeException("key为空");
        }
        int result = 0;
        for (long i = 0, j = this.strLen(key) * 8; i < j; i++) {
            if ("1".equals(this.getBit(key, i))) {
                result++;
            }
        }
        return result;
    }

    /**
     * BITMAP：设置指定Bitmap字符串中指定位置的值
     *
     * @param key    BitMap名称
     * @param offset 指定位置
     * @param val    除了 "1" 之外的字符串均视为 "0"
     */
    public void setBit(String key, long offset, String val) {
        if (key == null) {
            throw new RuntimeException("key为空");
        }
        stringOps.setBit(key, offset, "1".equals(val));
    }

    /**
     * LUA: 执行LUA脚本命令，固定LUA返回值类型为Long
     *
     * @param luaScript LUA脚本内容
     * @param keys      KEYS值列表
     * @param args      ARGV值数组
     * @return LUA脚本执行结果
     */
    public Long lua(String luaScript, List<String> keys, Object... args) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);
        return stringRedisTemplate.execute(redisScript, keys, args);
    }
    
    /**
     * 按前缀删除缓存
     *
     * @param prefix 缓存前缀（如 "user:info:"，自动匹配 "user:info:*"）
     */
    public void deleteByPrefix(String prefix) {
        // 用 KEYS 命令获取所有匹配的 key（核心：简单但阻塞风险高）
        Set<String> matchKeys = stringRedisTemplate.keys(prefix.trim() + "*");
        // 批量删除
        if (matchKeys != null && !matchKeys.isEmpty()) {
            stringRedisTemplate.delete(matchKeys);
        }
    }
}
