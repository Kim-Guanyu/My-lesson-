package com.mdkj.util;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import com.mdkj.entity.User;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserUtil {

    /**
     * 根据用户身份证号识别性别代码
     *
     * @param idcard 身份证号
     * @return 用户性别代码，1男，0女
     */
    public static int defaultGender(String idcard) {
        return IdcardUtil.getGenderByIdCard(idcard);
    }

    /**
     * 根据用户身份证号识别年龄
     *
     * @param idcard 身份证号
     * @return 用户年龄
     */
    public static int defaultAge(String idcard) {
        return IdcardUtil.getAgeByIdCard(idcard);
    }

    /**
     * 根据用户身份证号识别省份
     *
     * @param idcard 身份证号
     * @return 用户年省份
     */
    public static String defaultProvince(String idcard) {
        return IdcardUtil.getProvinceByIdCard(idcard);
    }

    /**
     * 根据用户身份证号识别星座
     *
     * @param idcard 身份证号
     * @return 用户星座
     */
    public static String defaultZodiac(String idcard) {
        int month = IdcardUtil.getMonthByIdCard(idcard);
        int date = IdcardUtil.getDayByIdCard(idcard);
        return DateUtil.getZodiac(month - 1, date);
    }

	/**
     * 根据用户身份证号识别用户属相并构建一个头像名称，格式为 "英文属相.后缀"
     *
     * @param idcard     身份证号
     * @param fileSuffix 文件后缀，如 png，jpg 等，不带点
     * @return 属相头像，格式为 `英文属相.后缀`
     */
    public static String defaultAvatar(String idcard, String fileSuffix) {
        int year = IdcardUtil.getYearByIdCard(idcard);
        Map<String, Object> map = new HashMap<>(12);
        map.put("鼠", "mouse");
        map.put("牛", "cow");
        map.put("虎", "tiger");
        map.put("兔", "rabbit");
        map.put("龙", "dragon");
        map.put("蛇", "snake");
        map.put("马", "horse");
        map.put("羊", "sheep");
        map.put("猴", "monkey");
        map.put("鸡", "chicken");
        map.put("狗", "dog");
        map.put("猪", "pig");
        return map.get(DateUtil.getChineseZodiac(year)) + "." + fileSuffix;
    }

    /**
     * 根据用户身份证号识别用户属相并构建一个头像名称，格式为 "英文属相.png"
     *
     * @param idcard 身份证号
     * @return 属相头像，格式为 `英文属相.png`
     */
    public static String defaultAvatar(String idcard) {
        return defaultAvatar(idcard, "png");
    }

    /**
     * 对用户记录进行脱敏（单条）
     *
     * @param user 用户实体，包含手机号码，身份证号，姓名和密码
     * @return 脱敏后的用户记录
     */
    public static User desensitization(User user) {

        // 身份证号脱敏规则: 前面显示6个数，后面显示3个数，其余都为星号
        String idcard = user.getIdcard();
        if(ObjectUtil.isNotNull(idcard)){
            user.setIdcard(DesensitizedUtil.idCardNum(idcard, 6, 3));
        }

        // 手机号码脱敏规则: 前面显示3个数，后面显示4个数，其余都为星号
        String phone = user.getPhone();
        if(ObjectUtil.isNotNull(phone)){
            user.setPhone(DesensitizedUtil.mobilePhone(phone));
        }

        // 真实姓名脱敏规则: 只显示姓
        String realname = user.getRealname();
        if(ObjectUtil.isNotNull(realname)){
            user.setRealname(DesensitizedUtil.chineseName(realname));
        }

        // 用户密码脱敏规则: 置空
        String password = user.getPassword();
        if(ObjectUtil.isNotNull(password)){
            user.setPassword("");
        }

        return user;
    }

    /**
     * 对用户记录进行脱敏（多条）
     *
     * @param users 用户实体列表，包含手机号码，身份证号，姓名和密码
     * @return 脱敏后的用户记录
     */
    public static List<User> desensitization(List<User> users) {
        return users.stream().peek(UserUtil::desensitization).collect(Collectors.toList());
    }
}
