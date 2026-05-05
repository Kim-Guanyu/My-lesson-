// 环境IP地址
const HOST = 'localhost';
const LINUX_HOST = '192.168.211.132';
const GATEWAY_HOST = `http://${HOST}:24101`;
const SOCKET_SERVER = `ws://${HOST}:24106`;
const MINIO_HOST = `http://${LINUX_HOST}:9000/my-lesson`;
const USER_AVATAR_URL = GATEWAY_HOST + '/user-server/api/v1/user/avatar/';
const MINIO_AVATAR = MINIO_HOST + '/avatar/';
const MINIO_BANNER = MINIO_HOST + '/banner/';
const MINIO_COURSE_COVER = MINIO_HOST + '/course-cover/';
const MINIO_COURSE_SUMMARY = MINIO_HOST + '/course-summary/';
const MINIO_EPISODE_VIDEO = MINIO_HOST + '/episode-video/';
const MINIO_EPISODE_VIDEO_COVER = MINIO_HOST + '/episode-video-cover/';
const MINIO_VIDEO = MINIO_HOST + '/video/';
const UPLOAD_AVATAR_URL = GATEWAY_HOST + '/user-server/api/v1/user/uploadAvatar/';

// 常用请求状态码
const STATUS = {
    SUCCESS: 1000
}

// 常用表单规则
const RULE = {
    TITLE: [{pattern: /^.{1,42}$/, message: '标题长度必须在1~42之间'}],
    AUTHOR: [{pattern: /^.{1,42}$/, message: '作者名称长度必须在1~42之间'}],
    INFO: [{pattern: /^.{1,170}$/, message: '描述长度必须在1~170之间'}],
    CONTENT: [{pattern: /^.{1,170}$/, message: '内容长度必须在1~170之间'}],
    VCODE: [{pattern: /^[0-9]{6}$/, message: '验证码必须是6位数字'}],
    MENU_URL: [{pattern: /^\/[a-zA-Z]{0,256}$/, message: '跳转地址必须以 / 开头，后续内容仅支持0~256个英文字母'}],
    MENU_ICON: [{pattern: /^[a-zA-Z]{1,256}$/, message: '图标仅支持1~256个英文字母'}],
    USERNAME: [{pattern: /^[a-zA-Z0-9]{4,20}$/, message: '账号必须由4到20个英文字母或数字组成'}],
    PASSWORD: [{pattern: /^[a-zA-Z0-9]{4,20}$/, message: '密码必须由4到20个英文字母或数字组成'}],
    REALNAME: [{pattern: /^[\u4e00-\u9fa5]{2,6}$/, message: '真实姓名必须由2到6个中文组成'}],
    NICKNAME: [{pattern: /^[\u4e00-\u9fa5|_a-zA-Z0-9]{2,10}$/, message: '昵称必须由2到10个中文、英文或数字组成'}],
    IDCARD: [{pattern: /^[1-9]\d{5}(19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '身份证号格式不正确'}],
    PHONE: [{pattern: /^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\d{8}$/, message: '手机号码格式不正确'}],
    EMAIL: [{pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '电子邮箱格式不正确'}],
    PROVINCE: [{pattern: /^[\u4e00-\u9fa5]{2,20}$/, message: '省份必须由2到20个中文组成'}],
    CODE: [{pattern: /^.{1,42}$/, message: '兑换口令长度必须在1~42之间'}],
    SN: [{pattern: /^.{1,42}$/, message: '订单编号长度必须在1~42之间'}],
}

// 项目大标题
const PROJECT_TITLE = '绝对精品课 - 我的课堂';

// 项目小标题
const PROJECT_SUB_TITLE = 'Welcome To Lesson Project';

// 省份选项
const PROVINCE_OPTIONS = [
    {
        id: 0,
        text: "华北地区",
        children: [{text: "北京市", id: "北京市"}, {text: "天津市", id: "天津市"}, {text: "河北省", id: "河北省"}, {text: "山西省", id: "山西省"}, {text: "内蒙古自治区", id: "内蒙古自治区"}]
    },
    {
        id: 1,
        text: "东北地区",
        children: [{text: "辽宁省", id: "辽宁省"}, {text: "吉林省", id: "吉林省"}, {text: "黑龙江省", id: "黑龙江省"}]
    },
    {
        id: 2,
        text: "华东地区",
        children: [{text: "上海市", id: "上海市"}, {text: "江苏省", id: "江苏省"}, {text: "浙江省", id: "浙江省"}, {text: "安徽省", id: "安徽省"}, {text: "福建省", id: "福建省"}, {text: "江西省", id: "江西省"}, {text: "山东省", id: "山东省"}]
    },
    {
        id: 3,
        text: "中南地区",
        children: [{text: "河南省", id: "河南省"}, {text: "湖北省", id: "湖北省"}, {text: "湖南省", id: "湖南省"}, {text: "广东省", id: "广东省"}, {text: "广西壮族自治区", id: "广西壮族自治区"}, {text: "海南省", id: "海南省"}]
    },
    {
        id: 4,
        text: "西南地区",
        children: [{text: "重庆市", id: "重庆市"}, {text: "四川省", id: "四川省"}, {text: "贵州省", id: "贵州省"}, {text: "云南省", id: "云南省"}, {text: "西藏自治区", id: "西藏自治区"}]
    },
    {
        id: 5,
        text: "西北地区",
        children: [{text: "陕西省", id: "陕西省"}, {text: "甘肃省", id: "甘肃省"}, {text: "青海省", id: "青海省"}, {text: "宁夏回族自治区", id: "宁夏回族自治区"}, {text: "新疆维吾尔自治区", id: "新疆维吾尔自治区"}]
    },
    {
        id: 6,
        text: "港澳台地区",
        children: [{text: "台湾省", id: "台湾省"}, {text: "香港特别行政区", id: "香港特别行政区"}, {text: "澳门特别行政区", id: "澳门特别行政区"}]
    }
]

// 星座选项
const ZODIAC_OPTIONS = [
    '白羊座（Aries）', '金牛座（Taurus）', '双子座（Gemini）',
    '巨蟹座（Cancer）', '狮子座（Leo）', '处女座（Virgo）',
    '天秤座（Libra）', '天蝎座（Scorpio）', '射手座（Sagittarius）',
    '摩羯座（Capricorn）', '水瓶座（Aquarius）', '双鱼座（Pisces）'
];

// 导出
module.exports = {
    GATEWAY_HOST, SOCKET_SERVER, STATUS, RULE,
    USER_AVATAR_URL,
    MINIO_AVATAR, MINIO_BANNER, MINIO_COURSE_COVER, MINIO_COURSE_SUMMARY,
    MINIO_EPISODE_VIDEO, MINIO_EPISODE_VIDEO_COVER, MINIO_VIDEO, UPLOAD_AVATAR_URL,
    PROJECT_TITLE, PROJECT_SUB_TITLE, PROVINCE_OPTIONS, ZODIAC_OPTIONS
}
