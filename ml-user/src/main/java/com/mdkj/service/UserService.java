package com.mdkj.service;

import com.mdkj.dto.*;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface UserService extends IService<User> {
    boolean insert(UserInsertDTO dto);
    User select(Long id);
    List<UserSimpleListVO> simpleList();
    PageVO<User> page(UserPageDTO dto);
    boolean update(UserUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    /**
     * 重置用户密码
     *
     * @param id 用户主键
     * @return 用户的默认密码
     */
    String resetPassword(Long id);
    /**
     * 修改用户密码
     *
     * @param dto 修改密码DTO
     * @return true修改成功，false修改失败
     */
    boolean updatePassword(UserUpdatePasswordDTO dto);
    /**
     * 获取用户记录的Excel数据
     *
     * @return 用户记录的Excel数据列表
     */
    List<UserExcelDTO> getExcelData();
    /**
     * 上传用户头像
     *
     * @param newFile 头像文件
     * @param id      用户主键
     * @return 文件名
     */
    String uploadAvatar(MultipartFile newFile, Long id);
    /**
     * 获取旧手机号码的解绑验证码
     *
     * @param id 用户主键
     * @return 一个随机的6位短信验证码
     */
    String getUnboundVcode(Long id);

    /**
     * 校验旧手机号码的解绑验证码
     *
     * @param id    用户主键
     * @param vcode 验证码
     * @return true表示验证码正确，false表示验证码错误
     */
    boolean checkUnboundVcode(Long id, String vcode);

    /**
     * 获取新手机号码的绑定验证码
     *
     * @param phone 新手机号码
     * @return 一个随机的6位短信验证码
     */
    String getBoundVcode(String phone);

    /**
     * 修改手机号码
     *
     * @param dto 用户修改手机号码DTO
     * @return true表示验证码正确，false表示验证码错误
     */
    boolean updatePhone(UserUpdatePhoneDTO dto);

    /**
     * 按账号密码登录系统
     *
     * @param dto 用户登录DTO
     * @return 返回登陆成功的用户记录，Token令牌，角色列表以及菜单列表
     */
    LoginVO loginByAccount(LoginByAccountDTO dto);

    /**
     * 根据手机号码生成一个随机的6位短信验证码
     *
     * @param phone 手机号码
     * @return 一个随机的6位短信验证码
     */
    String getVcode(String phone);

    /**
     * 根据手机号码 + 验证码登录
     *
     * @param dto 用户登录DTO
     * @return 返回登陆成功的用户记录，Token令牌以及菜单列表
     */
    LoginVO loginByPhone(LoginByPhoneDTO dto);
    /**
     * 统计用户数据，包括用户性别比例，日增用户数量，用户总数等
     *
     * @return 统计结果
     */
    Map<String, Object> statistics();








}
