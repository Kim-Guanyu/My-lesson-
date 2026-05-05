package com.mdkj.controller;

import com.mdkj.dto.*;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.EasyExcelUtil;
import com.mdkj.util.ML;
import com.mdkj.util.MinioUtil;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdkj.entity.User;
import com.mdkj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户表 控制层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@RestController
@Tag(name = "用户表接口")
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Resource
    private MinioUtil minioUtil;

    /**
     * 添加用户表。
     *
     * @param user 用户表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存用户表")
    public boolean save(@RequestBody @Parameter(description="用户表")User user) {
        return userService.save(user);
    }

    /**
     * 根据主键删除用户表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键用户表")
    public boolean remove(@PathVariable @Parameter(description="用户表主键")Long id) {
        return userService.removeById(id);
    }


    /**
     * 查询所有用户表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有用户表")
    public List<User> list() {
        return userService.list();
    }

    /**
     * 根据用户表主键获取详细信息。
     *
     * @param id 用户表主键
     * @return 用户表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取用户表")
    public User getInfo(@PathVariable Long id) {
        return userService.getById(id);
    }


    @Operation(summary = "新增 - 单条新增", description = "新增一条用户记录")
    @PostMapping("insert")
    public boolean insert(@Validated @RequestBody UserInsertDTO dto) {
        return userService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条用户记录")
    @GetMapping("select/{id}")
    public User select(@PathVariable("id") Long id) {
        return userService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部用户记录，仅返回简单信息")
    @GetMapping("simpleList")
    public List<UserSimpleListVO> simpleList() {
        return userService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询用户记录")
    @GetMapping("page")
    public PageVO<User> page(@Validated UserPageDTO dto) {
        return userService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条用户记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody UserUpdateDTO dto) {
        return userService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条用户记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return userService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除用户记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return userService.deleteBatch(ids);
    }

    @Operation(summary = "修改 - 重置密码", description = "按主键重置用户的登录密码为默认密码，重置成功后返回默认密码")
    @PutMapping("resetPassword/{id}")
    public Result<String> resetPassword(@PathVariable("id") Long id) {
        return new Result<>(userService.resetPassword(id));
    }

    @Operation(summary = "修改 - 登录密码", description = "按主键修改用户的登录密码")
    @PutMapping("updatePassword")
    public boolean updatePassword(@Validated @RequestBody UserUpdatePasswordDTO dto) {
        return userService.updatePassword(dto);
    }

    @Operation(summary = "查询 - 报表打印", description = "打印用户相关的报表数据")
    @GetMapping("excel")
    public void excel(HttpServletResponse response) {
        EasyExcelUtil.download(response, "用户统计表", userService.getExcelData());
    }

    @Operation(summary = "修改 - 用户头像", description = "按主键修改用户的头像")
    @PostMapping("uploadAvatar/{id}")
    public Result<String> uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile,
                                       @PathVariable("id") Long id) {
        return new Result<>(userService.uploadAvatar(avatarFile, id));
    }

    @Operation(summary = "查询 - 用户头像", description = "按文件名读取用户头像，通过网关代理输出")
    @GetMapping("avatar/{fileName:.+}")
    public ResponseEntity<byte[]> avatar(@PathVariable("fileName") String fileName) {
        return this.buildAvatarResponse(fileName);
    }

    @Operation(summary = "查询 - 解绑验证码", description = "获取旧手机号码的解绑验证码")
    @GetMapping("getUnboundVcode/{id}")
    public Result<String> getUnboundVcode(@PathVariable("id") Long id) {
        return new Result<>(userService.getUnboundVcode(id));
    }

    @Operation(summary = "校验 - 解绑验证码", description = "校验旧手机号码的解绑验证码")
    @GetMapping("checkUnboundVcode/{id}/{vcode}")
    public boolean checkUnboundVcode(@PathVariable("id") Long id,
                                     @PathVariable("vcode") String vcode) {
        return userService.checkUnboundVcode(id, vcode);
    }

    @Operation(summary = "查询 - 绑定验证码", description = "获取新手机号码的绑定验证码")
    @GetMapping("getBoundVcode/{phone}")
    public Result<String> getBoundVcode(@PathVariable("phone") String phone) {
        return new Result<>(userService.getBoundVcode(phone));
    }

    @Operation(summary = "修改 - 手机号码", description = "修改用户的手机号码")
    @PutMapping("updatePhone")
    public boolean updatePhone(@Validated @RequestBody UserUpdatePhoneDTO dto) {
        return userService.updatePhone(dto);
    }

    @Operation(summary = "登录 - 账号密码", description = "按账号和密码登录系统")
    @PostMapping("loginByAccount")
    public LoginVO loginByAccount(@Validated @RequestBody LoginByAccountDTO dto) {
        return userService.loginByAccount(dto);
    }

    @Operation(summary = "查询 - 登录验证码", description = "获取手机号码的验证码")
    @GetMapping("getVcode/{phone}")
    public Result<String> getVcode(@PathVariable("phone") String phone) {
        return new Result<>(userService.getVcode(phone));
    }

    @Operation(summary = "登录 - 手机号码", description = "按手机号码和验证码登录系统")
    @PostMapping("loginByPhone")
    public LoginVO loginByPhone(@Validated @RequestBody LoginByPhoneDTO dto) {
        return userService.loginByPhone(dto);
    }

    @Operation(summary = "查询 - 统计数据", description = "查询用户相关的统计数据")
    @GetMapping("statistics")
    public Map<String, Object> statistics() {
        return userService.statistics();
    }

    private ResponseEntity<byte[]> buildAvatarResponse(String fileName) {
        String objectName = ML.MinIO.AVATAR_DIR + "/" + fileName;
        try {
            return this.buildImageResponse(objectName);
        } catch (ServiceException e) {
            if (ML.User.DEFAULT_AVATARS.contains(fileName)) {
                throw e;
            }
            return this.buildImageResponse(ML.MinIO.AVATAR_DIR + "/" + ML.User.DEFAULT_AVATARS.get(0));
        }
    }

    private ResponseEntity<byte[]> buildImageResponse(String objectName) {
        String contentType = minioUtil.getObjectContentType(ML.MinIO.BUCKET_NAME, objectName);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            mediaType = MediaType.parseMediaType(contentType);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .contentType(mediaType)
                .body(minioUtil.getObjectBytes(ML.MinIO.BUCKET_NAME, objectName));
    }










}
