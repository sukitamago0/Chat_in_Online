package com.easychat.controller;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.IOException;

@RestController
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController {
    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/saveUserInfo")
    @GlobalInterceptor
    public ResponseVO saveUserInfo(HttpServletRequest request, UserInfo userInfo,
                                   MultipartFile avatarFile,
                                   MultipartFile avatarCover) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userInfo.setUserId(tokenUserInfoDto.getUserId());
        //由于该方法传入UserInfo 为了防止前端绕开直接修改关键数据 故要对几个关键参数set null以防止前端注入
        userInfo.setPassword(null);
        userInfo.setStatus(null);
        userInfo.setCreateTime(null);
        userInfo.setLastLoginTime(null);
        this.userInfoService.updateUserInfo(userInfo, avatarFile, avatarCover);
        return getSuccessResponseVO(getUserInfo(request));//直接调用查询数据方法查询修改后数据返回
    }

    @RequestMapping("/updatePassword")
    @GlobalInterceptor
    public ResponseVO updatePassword(HttpServletRequest request,
                                     @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeMd5(password));
        this.userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId());
        // TODO 强踢退出，重新登录
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        // TODO 退出登录，关闭WebSocket连接
        return getSuccessResponseVO(null);
    }




}
