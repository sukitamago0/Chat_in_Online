package com.easychat.controller;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.po.UserContactApply;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.enums.PageSize;
import com.easychat.enums.ResponseCodeEnum;
import com.easychat.enums.UserContactStatusEnum;
import com.easychat.enums.UserContactTypeEnum;
import com.easychat.exception.BusinessException;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import io.swagger.models.auth.In;
import jodd.util.ArraysUtil;
import org.apache.ibatis.reflection.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class UserContactController extends ABaseController {

    private static final Logger log = LoggerFactory.getLogger(UserContactController.class);
    @Resource
    private UserContactService userContactService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserContactApplyService userContactApplyService;

    @RequestMapping("/search")
    @GlobalInterceptor
    // 搜索联系人
    public ResponseVO search(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactSearchResultDto userContactSearchResultDto = userContactService.searchContact(tokenUserInfoDto.getUserId(), contactId);
        return getSuccessResponseVO(userContactSearchResultDto);
    }

    @RequestMapping("/applyAdd")
    @GlobalInterceptor
    // 提交添加好友申请
    public ResponseVO applyAdd(HttpServletRequest request, @NotEmpty String contactId, String applyMsg) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        Integer joinType = userContactService.applyAdd(tokenUserInfoDto, contactId, applyMsg);
        return getSuccessResponseVO(joinType);
    }

    @RequestMapping("/loadApply")
    @GlobalInterceptor
    // 提交添加好友申请
    public ResponseVO applyAdd(HttpServletRequest request, Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactApplyQuery applyQuery = new UserContactApplyQuery();

        applyQuery.setOrderBy("last_apply_time desc");
        applyQuery.setReceiveUserId(tokenUserInfoDto.getUserId());
        applyQuery.setPageNo(pageNo);
        applyQuery.setPageSize(PageSize.SIZE15.getSize());
        applyQuery.setQueryContactInfo(true);
        PaginationResultVO resultVO = userContactApplyService.findListByPage(applyQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/dealWithApply")
    @GlobalInterceptor
    // 提交添加好友申请
    public ResponseVO dealWithApply(HttpServletRequest request, @NotNull Integer applyId, @NotNull Integer status) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        this.userContactApplyService.dealWithApply(tokenUserInfoDto.getUserId(), applyId, status);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVO loadContact(HttpServletRequest request, @NotNull String contactType) {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
        if(null == contactTypeEnum){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactQuery contactQuery = new UserContactQuery();
        contactQuery.setUserId(tokenUserInfoDto.getUserId());
        contactQuery.setContactType(contactTypeEnum.getType());
        if(contactTypeEnum.equals(UserContactTypeEnum.USER)){
            contactQuery.setQueryContactUserInfo(true);
        } else if (contactTypeEnum.equals(UserContactTypeEnum.GROUP)){
            contactQuery.setQueryGroupInfo(true);
            contactQuery.setExcludeMyGroup(true);
        }
        contactQuery.setOrderBy("last_update_time desc");
        //避免看到
        contactQuery.setStatusArray(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
        });
        List<UserContact> contactList = userContactService.findListByParam(contactQuery);
        return getSuccessResponseVO(contactList);
    }

    //该方法的逻辑有些乱 不用较真 是为了某个前端请求而服务的
    //不一定是好友时调用 例如群聊查看
    @RequestMapping("/getContacInfo")
    @GlobalInterceptor
    public ResponseVO getContactInfo(HttpServletRequest request, @NotNull String contactId) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(),contactId);
        if(userContact != null){
            userInfoVO.setContactStatus(UserContactStatusEnum.FRIEND.getStatus());
        }
        return getSuccessResponseVO(userInfoVO);
    }

    //该方法的逻辑有些乱 不用较真 是为了某个前端请求而服务的
    //一定是好友时调用 
    @RequestMapping("/getContactUserInfo")
    @GlobalInterceptor
    public ResponseVO getContactUserInfo(HttpServletRequest request, @NotNull String contactId) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);//判断是否有好友关系
        if(null == userContact || !ArraysUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST.getStatus(),
        }, userContact.getStatus())){//只有非好友情况 或者已添加 被拉黑 和被删除 能够查看
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO);
    }

    /*
    *删除联系人
     */
    @RequestMapping("/delContact")
    @GlobalInterceptor
    public ResponseVO delContact(HttpServletRequest request, @NotNull String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(),contactId,UserContactStatusEnum.DEL);
        return getSuccessResponseVO(null);
    }

    /*
     *拉黑
     */
    @RequestMapping("/addContact2BlackList")
    @GlobalInterceptor
    public ResponseVO addContact2BlackList(HttpServletRequest request, @NotNull String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(),contactId,UserContactStatusEnum.BLACKLIST);
        return getSuccessResponseVO(null);
    }


}
