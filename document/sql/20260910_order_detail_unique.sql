-- 同一张订单里同一门课程只应有一条明细，由数据库兜住最后一道防线。
-- 背景：prePay 早期每次调用都无条件新建订单与明细，反复点「立即购买」会攒下
--       重复数据；应用层已改为复用待付款订单，这里补上索引防止再次发生。
-- 执行前先确认下面的重复检查返回空集，否则需先清理重复明细（保留 min(id) 那条）。
SELECT fk_order_id, fk_course_id, COUNT(*) AS cnt
FROM ml_oms.order_detail
GROUP BY fk_order_id, fk_course_id
HAVING COUNT(*) > 1;

-- 如上面有输出，可用下面这条清理（只保留每组最早的一条）：
-- DELETE d FROM ml_oms.order_detail d
--     JOIN (SELECT fk_order_id, fk_course_id, MIN(id) AS keep_id
--           FROM ml_oms.order_detail
--           GROUP BY fk_order_id, fk_course_id
--           HAVING COUNT(*) > 1) t
--       ON d.fk_order_id = t.fk_order_id
--      AND d.fk_course_id = t.fk_course_id
--      AND d.id <> t.keep_id;

ALTER TABLE ml_oms.order_detail
    ADD UNIQUE KEY uk_order_detail_order_course (fk_order_id, fk_course_id);
