package com.mdkj.util;

import lombok.Getter;


@Getter
public enum ResultCode {
    // 成功
	SUCCESS(1000, "请求成功"),
	// 服务器错误
	SERVER_ERROR(2000, "服务器异常"),
	MYSQL_ERROR(2001, "数据操作失败"),
	MINIO_ERROR(2002, "文件操作失败"),
	// 请求参数错误
	ILLEGAL_PARAM(3000, "请求参数错误"),
	ACCOUNT_ILLEGAL(3001, "账号密码错误"),
	ID_CARD_ILLEGAL(3002, "身份证号错误"),
	OLD_PASSWORD_ILLEGAL(3003, "旧密码错误"),
	VCODE_ILLEGAL(3004, "验证码错误"),
	DATETIME_ILLEGAL(3005, "时间设定错误"),
	// 记录重复
	REPEAT_RECORD(4000, "记录重复"),
	USERNAME_REPEAT(4001, "登录账号重复"),
	ID_CARD_REPEAT(4002, "身份证号重复"),
	PHONE_REPEAT(4003, "手机号码重复"),
	EMAIL_REPEAT(4004, "电子邮箱重复"),
	TITLE_REPEAT(4005, "标题重复"),
	CODE_REPEAT(4006, "兑换口令重复"),
	SECKILL_DETAIL_REPEAT(4007, "秒杀商品重复"),
	CART_REPEAT(4008, "购物车记录重复"),
	ORDER_DETAIL_REPEAT(4009, "订单明细重复"),
	SECKILL_REPEAT(4010, "秒杀活动重复"),
	// 记录不存在
	RECORD_NOT_FOUND(5000, "记录不存在"),
	USER_NOT_FOUND(5001, "用户不存在"),
	ROLE_NOT_FOUND(5002, "角色不存在"),
	MENU_NOT_FOUND(5003, "菜单不存在"),
	CATEGORY_NOT_FOUND(5004, "课程类别不存在"),
	COURSE_NOT_FOUND(5005, "课程不存在"),
	SEASON_NOT_FOUND(5006, "课程季次不存在"),
	EPISODE_NOT_FOUND(5007, "课程集次不存在"),
	COMMENT_NOT_FOUND(5008, "评论不存在"),
	REPORT_NOT_FOUND(5009, "举报不存在"),
	FOLLOW_NOT_FOUND(5010, "收藏不存在"),
	NOTICE_NOT_FOUND(5011, "通知不存在"),
	BANNER_NOT_FOUND(5012, "横幅不存在"),
	ARTICLE_NOT_FOUND(5013, "新闻不存在"),
	SECKILL_NOT_FOUND(5014, "秒杀活动不存在"),
	COUPONS_NOT_FOUND(5015, "优惠卷不存在"),
	CART_NOT_FOUND(5016, "购物车不存在"),
	ORDER_NOT_FOUND(5017, "订单不存在"),
	PHONE_NOT_FOUND(5018, "手机号码不存在"),
	SECKILL_DETAIL_NOT_FOUND(5019, "秒杀明细不存在"),
	ORDER_DETAIL_NOT_FOUND(5020, "订单明细不存在"),
	// 远程调用微服务失败
	OPEN_FEIGN_ERROR(6000, "远程调用失败"),
	// 友好提示
	SECKILL_NOT_START(7000, "活动未开始"),
	SECKILL_END(7001, "活动已结束");

    private final int CODE;
    private final String MESSAGE;

    ResultCode(int code, String msg) {
        this.CODE = code;
        this.MESSAGE = msg;
    }
}
