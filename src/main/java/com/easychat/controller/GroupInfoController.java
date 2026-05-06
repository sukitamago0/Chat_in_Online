package com.easychat.controller;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.GroupInfoVo;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.GroupInfo;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.query.GroupInfoQuery;
import com.easychat.enums.PageSize;
import com.easychat.exception.BusinessException;
import com.easychat.service.GroupInfoService;
import com.easychat.service.UserContactService;
import org.apache.catalina.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @Description:Controller
 * @author:t1
 * @date:2025/10/22
 */

@RestController
@RequestMapping("/group")
public class GroupInfoController extends ABaseController {

	@Resource
	private GroupInfoService groupInfoService;

	@Resource
	private UserContactService userContactService;


	@RequestMapping("/saveGroup")
	@GlobalInterceptor
	@Validated
	//保存群信息
	public ResponseVO saveGroup(HttpServletRequest request,
								String groupId,
								@NotEmpty String groupName,
								String groupNotice,
								@NotNull Integer joinType,
								MultipartFile avatarFile,
								MultipartFile avatarCover) throws IOException {

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

		GroupInfo groupInfo = new GroupInfo();
		groupInfo.setGroupId(groupId);
		groupInfo.setGroupName(groupName);
		groupInfo.setGroupNotice(groupNotice);
		groupInfo.setJoinType(joinType);
		groupInfo.setGroupOwnerId(tokenUserInfoDto.getUserId());

		this.groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);


		return getSuccessResponseVO(null);
	}

	@RequestMapping("/loadMyGroup")
	@GlobalInterceptor
	//查询我的群聊列表
	public ResponseVO loadMyGroup(HttpServletRequest request) throws IOException {

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
		groupInfoQuery.setGroupOwnerId(tokenUserInfoDto.getUserId());
		groupInfoQuery.setOrderBy("create_time desc");
		List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(groupInfoQuery);
		return getSuccessResponseVO(groupInfoList);
	}

	@RequestMapping("/getGroupInfo")
	@GlobalInterceptor
	//根据群id查询群信息
	public ResponseVO getGroupInfo(HttpServletRequest request, @NotEmpty String groupId){
		GroupInfo groupInfo = this.getGroupdetailCommon(request, groupId);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		//查询群成员数
		Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
		groupInfo.setMemberCount(memberCount);
		return getSuccessResponseVO(groupInfo);
	}

	private GroupInfo getGroupdetailCommon(HttpServletRequest request, String groupId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

		UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), groupId);
		if (null == userContact || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())) {
			throw new BusinessException("您不是该群的成员或群聊已解散");
		}
		GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
		if(null == groupInfo || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())){
			throw new BusinessException("群聊不存在或已解散");
		}
		return groupInfo;
	}

	@RequestMapping("/getGroupInfo4Chat")
	@GlobalInterceptor
	//根据群id查询群成员信息
	public ResponseVO getGroupInfo4Chat(HttpServletRequest request, @NotEmpty String groupId){
		GroupInfo groupInfo = this.getGroupdetailCommon(request, groupId);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setQueryUserInfo(true);
		userContactQuery.setOrderBy("create_time asc");
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);
		GroupInfoVo groupInfoVo = new GroupInfoVo();
		groupInfoVo.setGroupInfo(groupInfo);
		groupInfoVo.setUserContactList(userContactList);
		return getSuccessResponseVO(groupInfoVo);
	}



}