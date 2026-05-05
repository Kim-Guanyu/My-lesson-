package com.mdkj.util;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class ML {

    public interface Redis {
        /** 手机登录验证码Key值前缀 */
        String LOGIN_VCODE_PREFIX = "login_vcode:";
        /** 解绑验证码Key值前缀 */
        String UNBOUND_VCODE_PREFIX = "unbound_vcode:";
        /** 绑定验证码Key值前缀 */
        String BOUND_VCODE_PREFIX = "bound_vcode:";
        /** 用户统计数据Key值 */
        String USER_STATISTICS_DATA_KEY = "user_statistics_data";
        /** 订单统计数据Key值 */
        String ORDER_STATISTICS_DATA_KEY = "order_statistics_data";
        /** 前N条通知记录Key值 */
        String TOP_NOTICE_KEY_PREFIX = "top_notice:";
        /** 前N条新闻记录Key值 */
        String TOP_ARTICLE_KEY_PREFIX = "top_article:";
        /** 前N条横幅记录Key值 */
        String TOP_BANNER_KEY_PREFIX = "top_banner:";
        /** 秒杀活动商品详情缓存前缀 */
        String SECKILL_COURSE_INFO_PREFIX = "seckill:course_info:";
        /** 秒杀活动商品库存缓存前缀 */
        String SECKILL_COURSE_COUNT_PREFIX = "seckill:course_count:";
    }

    public interface MinIO {
        /** 桶名 */
        String BUCKET_NAME = "my-lesson";
        /** 头像上传目录 */
        String AVATAR_DIR = "avatar";
        /** 课程封面上传目录 */
        String COURSE_COVER_DIR = "course-cover";
        /** 课程摘要上传目录 */
        String COURSE_SUMMARY_DIR = "course-summary";
        /** 集次视频封面上传目录 */
        String EPISODE_VIDEO_COVER_DIR = "episode-video-cover";
        /** 集次视频上传目录 */
        String EPISODE_VIDEO_DIR = "episode-video";
        /** 轮播图片上传目录 */
        String BANNER_DIR = "banner";
    }

    public interface Banner {
        /** 横幅默认图片 */
        String DEFAULT_BANNER = "default-banner.jpg";
    }

    public interface Order {
        /** 订单状态: 0未付款，1已付款，2已取消，3其他 */
        Integer UNPAID = 0, PAID = 1, CANCEL = 2, OTHER_STATE = 3;
        /** 订单支付方式: 0未支付，1微信，2支付宝，3其他 */
        Integer NO_PAY = 0, WECHAT_PAY = 1, ALI_PAY = 2, OTHER_PAY = 3;

        /** 订单状态处理方法：数字代码 -> 字符串 */
        static String statusFormat(Integer statusCode) {
            if (Objects.equals(statusCode, UNPAID)) return "未付款";
            if (Objects.equals(statusCode, PAID)) return "已付款";
            if (Objects.equals(statusCode, CANCEL)) return "已取消";
            if (Objects.equals(statusCode, OTHER_STATE)) return "其他";
            return "异常";
        }

        /** 订单支付方式处理方法：数字代码 -> 字符串 */
        static String payTypeFormat(Integer payTypeCode) {
            if (Objects.equals(payTypeCode, NO_PAY)) return "未支付";
            if (Objects.equals(payTypeCode, WECHAT_PAY)) return "微信";
            if (Objects.equals(payTypeCode, ALI_PAY)) return "支付宝";
            if (Objects.equals(payTypeCode, OTHER_PAY)) return "其他";
            return "异常";
        }
    }

    public interface Course {
        /** 课程摘要默认图片 */
        String DEFAULT_SUMMARY = "default-course-summary.jpg";
        /** 课程封面默认图片 */
        String DEFAULT_COVER = "default-course-cover.jpg";
    }

    public interface Episode {
        /** 集次默认视频 */
        String DEFAULT_VIDEO = "default-episode-video.mp4";
        /** 集次默认视频封面图 */
        String DEFAULT_VIDEO_COVER = "default-episode-video-cover.jpg";
    }

    public interface User {
        /** 用户性别: 0女，1男，2保密 */
        Integer FEMALE = 0, MALE = 1, SECRET = 2;
        /** 默认密码 */
        String DEFAULT_PASSWORD = "123456789";
        /** 用户默认头像: 12生肖图 */
        List<String> DEFAULT_AVATARS = Arrays.asList(
                "mouse.jpg", "cow.jpg", "tiger.jpg", "rabbit.jpg",
                "dragon.jpg", "snake.jpg", "horse.jpg", "sheep.jpg",
                "monkey.jpg", "chicken.jpg", "dog.jpg", "pig.jpg");

        /** 用户性别处理方法：数字代码 -> 字符串 */
        static String genderFormat(Integer genderCode) {
            if (Objects.equals(genderCode, FEMALE)) return "女孩";
            if (Objects.equals(genderCode, MALE)) return "男孩";
            if (Objects.equals(genderCode, SECRET)) return "保密";
            return "异常";
        }
    }

    public interface Menu {
        /** 根菜单ID */
        Long ROOT_ID = 0L;
    }

    public interface Seckill {
        /** 秒杀活动状态: 0未开始，1已开始，2已结束 */
        Integer NOT_START = 0, STARTED = 1, ENDED = 2;

        /** 秒杀活动状态处理方法：数字代码 -> 字符串 */
        static String statusFormat(Integer seckillCode) {
            if (Objects.equals(seckillCode, NOT_START)) return "未开始";
            if (Objects.equals(seckillCode, STARTED)) return "已开始";
            if (Objects.equals(seckillCode, ENDED)) return "已结束";
            return "异常";
        }
    }

    public interface Regex {
        String TITLE_RE = "^.{1,42}$", TITLE_RE_MSG = "标题长度必须在1~42之间";
        String AUTHOR_RE = "^.{1,42}$", AUTHOR_RE_MSG = "作者名称长度必须在1~42之间";
        String INFO_RE = "^.{1,170}$", INFO_RE_MSG = "描述长度必须在1~170之间";
        String CONTENT_RE = "^.{1,170}$", CONTENT_RE_MSG = "内容长度必须在1~170之间";
        String VCODE_RE = "^[0-9]{6}$", VCODE_RE_MSG = "验证码必须是6位数字";
        String MENU_URL_RE = "^/[a-zA-Z]{0,256}$", MENU_URL_RE_MSG = "跳转地址必须以 / 开头，后续内容仅支持0~256个英文字母";
        String MENU_ICON_RE = "^[a-zA-Z]{1,256}$", MENU_ICON_RE_MSG = "图标仅支持1~256个英文字母";
        String USERNAME_RE = "^[a-zA-Z0-9]{4,20}$", USERNAME_RE_MSG = "账号必须由4到20个英文字母或数字组成";
        String PASSWORD_RE = "^[a-zA-Z0-9]{4,20}$", PASSWORD_RE_MSG = "密码必须由4到20个英文字母或数字组成";
        String REALNAME_RE = "^[\\u4e00-\\u9fa5]{2,6}$", REALNAME_RE_MSG = "真实姓名必须由2到6个中文组成";
        String NICKNAME_RE = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,10}$", NICKNAME_RE_MSG = "昵称必须由2到10个中文、英文或数字组成";
        String ID_CARD_RE = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", ID_CARD_RE_MSG = "身份证号格式不正确";
        String PHONE_RE = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$", PHONE_RE_MSG = "手机号码格式不正确";
        String EMAIL_RE = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", EMAIL_RE_MSG = "电子邮箱格式不正确";
        String PROVINCE_RE = "^[\\u4e00-\\u9fa5]{2,20}$", PROVINCE_RE_MSG = "省份必须由2到20个中文组成";
        String CODE_RE = "^.{1,42}$", CODE_RE_MSG = "兑换口令长度必须在1~42之间";
        String SN_RE = "^.{1,42}$", SN_RE_MSG = "兑换口令长度必须在1~42之间";
    }
}

