-- MQ 重投时，订单号 sn 必须由数据库提供最终幂等保障。
-- 执行前先确认下面的重复检查返回空集。
SELECT sn, COUNT(*) AS cnt
FROM ml_oms.`order`
GROUP BY sn
HAVING COUNT(*) > 1;

ALTER TABLE ml_oms.`order`
    ADD UNIQUE KEY uk_order_sn (sn);
