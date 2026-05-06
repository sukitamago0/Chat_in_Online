package com.easychat.service.Impl;

import com.easychat.controller.UserContactController;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.*;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;

import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import jodd.util.ArraysUtil;

import org.springframework.stereotype.Service;
import com.easychat.service.UserContactService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:联系人Service
 * @author:t1
 * @date:2025/10/22
 */

@Service("userContactService")
public class UserContactServiceImpl implements UserContactService{

	@Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private ChatMessageMapper<ChatMessage,ChatMessageQuery> chatMessageMapper;

	@Resource
	private MessageHandler messageHandler;

	@Resource
	private ChannelContextUtils channelContextUtils;

	/**
	 * 根据条件查询列表
	 */
	public List<UserContact> findListByParam(UserContactQuery query) {
		return this.userContactMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserContactQuery query) {
		return this.userContactMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserContact> findListByPage(UserContactQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserContact> list = this.findListByParam(query);
		PaginationResultVO<UserContact> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(UserContact bean) {
		return this.userContactMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserContact> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<UserContact> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据UserIdAndContactId查询
	 */
	public UserContact getUserContactByUserIdAndContactId(String userId,String contactId) {
		return this.userContactMapper.selectByUserIdAndContactId(userId,contactId);
	}

	/**
	 * 根据UserIdAndContactId更新
	 */
	public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId,String contactId) {
		return this.userContactMapper.updateByUserIdAndContactId(bean,userId,contactId);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	public Integer deleteUserContactByUserIdAndContactId(String userId,String contactId) {
		return this.userContactMapper.deleteByUserIdAndContactId(userId,contactId);
	}

	@Override
	public UserContactSearchResultDto searchContact(String userId, String contactId) {

		UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if (typeEnum == null) {
			return null;
		}
		UserContactSearchResultDto resultDto = new UserContactSearchResultDto();
		switch (typeEnum) {
			case USER:
				UserInfo userInfo = userInfoMapper.selectByUserId(contactId);
				if(userInfo == null) {
					return null;
				}
				resultDto = CopyTools.copy(userInfo, UserContactSearchResultDto.class);
				break;
			case GROUP:
				GroupInfo groupInfo = groupInfoMapper.selectByGroupId(contactId);
				if(groupInfo == null) {
					return null;
				}
				resultDto.setNickName(groupInfo.getGroupName());
				break;
		}

		resultDto.setContactType(typeEnum.toString());
		resultDto.setContactId(contactId);

		//如果搜索自己则直接设置为好友并返回状态
		if(userId.equals(contactId)){
			resultDto.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			return  resultDto;
		}

		//查询是否是好友关系
		UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
		resultDto.setStatus(userContact == null ? null : userContact.getStatus());

		return resultDto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer applyAdd(TokenUserInfoDto tokenUserInfoDto, String contactId, String applyInfo) {
		UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if (typeEnum == null) {
			//此处传入的contactId一定是有前缀的 故为空则直接报错
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//申请人
		String applyUserId = tokenUserInfoDto.getUserId();

		//若无申请信息则使用默认申请信息
		applyInfo = StringTools.isEmpty(applyInfo) ? String.format(Constants.APPLY_INFO_TEMPLATE,tokenUserInfoDto.getNickName()) : applyInfo;

		Long curTime = System.currentTimeMillis();

		Integer joinType = null;//加入类型（在申请加群时为群类型）
		String receiveUserId = contactId;//接收人是谁（在申请加群时为群主）

		//查询对方是否已经添加 如果已经拉黑无法添加
		UserContact userContact = userContactMapper.selectByUserIdAndContactId(applyUserId, contactId);
		if(userContact != null &&
				ArraysUtil.contains(new Integer[]{
						UserContactStatusEnum.BLACKLIST_BE.getStatus(),
						UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus() ,
				},userContact.getStatus())){
			throw new BusinessException("对方已拉黑您，无法添加");
		}

		if(UserContactTypeEnum.GROUP == typeEnum) {
			GroupInfo groupInfo = groupInfoMapper.selectByGroupId(contactId);
			if(groupInfo == null || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())) {
				throw new BusinessException("群不存在或已解散");
			}
			receiveUserId = groupInfo.getGroupOwnerId();
			joinType = groupInfo.getJoinType();
		} else {
			UserInfo userInfo = userInfoMapper.selectByUserId(contactId);
			if(userInfo == null) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			joinType = userInfo.getJoinType();
		}

		//直接加入：不用添加申请记录
		if(JoinTypeEnum.JOIN.getType().equals(joinType)) {
			//添加联系人
			this.addContact(applyUserId, receiveUserId, contactId, typeEnum.getType(), applyInfo);
			return joinType;
		}

		//不能直接加入：查看是否已经存在申请
		UserContactApply dbApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
		if(dbApply == null) {
			UserContactApply contactApply = new UserContactApply();
			contactApply.setApplyUserId(applyUserId);
			contactApply.setContactType(typeEnum.getType());
			contactApply.setReceiveUserId(receiveUserId);
			contactApply.setLastApplyTime(curTime);
			contactApply.setContactId(contactId);
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			contactApply.setApplyInfo(applyInfo);
			this.userContactApplyMapper.insert(contactApply);
		} else {
			//如果申请已经存在 则更新状态
			UserContactApply contactApply = new UserContactApply();
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			contactApply.setApplyInfo(applyInfo);
			contactApply.setLastApplyTime(curTime);
			this.userContactApplyMapper.updateByApplyId(contactApply, dbApply.getApplyId());
		}

		if(dbApply == null || !UserContactApplyStatusEnum.INIT.getStatus().equals(dbApply.getStatus())) {
			//发送ws消息
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
			messageSendDto.setContactId(contactId);
			messageSendDto.setContactId(receiveUserId);
			messageHandler.sendMessage(messageSendDto);
		}

		return joinType;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
		//移除好友 不做物理删除 只改变表状态
		UserContact userContact = userContactMapper.selectByUserIdAndContactId(userId, contactId);
		userContact.setStatus(statusEnum.getStatus());
		userContactMapper.updateByUserIdAndContactId(userContact,userId,contactId);

		//让被删除的好友也删除自己

		UserContact friendContact = new UserContact();
		if (UserContactStatusEnum.DEL == statusEnum) {//若删除好友 则好友被删除
			friendContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
		} else if (UserContactStatusEnum.BLACKLIST == statusEnum) {
			friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
		}
		userContactMapper.updateByUserIdAndContactId(friendContact, contactId, userId);

		// TODO 从我的好友列表缓存中删除好友
		// TODO 从好友列表缓存中删除我

	}

	@Override
	public void addContact(String applyUserId, String receriveUserId, String contactId, Integer contactType, String applyInfo) {
		//群聊
		if(UserContactTypeEnum.GROUP.getType().equals(contactType)){
			UserContactQuery contactQuery = new UserContactQuery();
			contactQuery.setContactId(contactId);
			contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer count = userContactMapper.selectCount(contactQuery);
			SysSettingDto systemSettingDto = redisComponent.getSysSetting();
			if(count >= systemSettingDto.getMaxGroupMemberCount()){
				throw new BusinessException("群成员已满，无法加入");
			}
		}
		Date curDate = new Date();
		//同意 双方添加好友
		List<UserContact> contactList = new ArrayList<>();
		//申请人添加被申请对象
		UserContact userContact = new UserContact();
		userContact.setUserId(applyUserId);
		userContact.setContactId(contactId);
		userContact.setContactType(contactType);//是好友还是群组
		userContact.setCreateTime(curDate);
		userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		userContact.setLastUpdateTime(curDate);
		contactList.add(userContact);

		//若是申请好友 则被申请对象添加申请人 若被申请对象为群组则不用添加对方为好友
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
			userContact = new UserContact();
			userContact.setUserId(receriveUserId);
			userContact.setContactId(applyUserId);
			userContact.setContactType(contactType);//是好友还是群组
			userContact.setCreateTime(curDate);
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setLastUpdateTime(curDate);
			contactList.add(userContact);
		}
		//批量插入
		userContactMapper.insertOrUpdateBatch(contactList);

		redisComponent.addUserContact(applyUserId, contactId);

		//创建会话
		String sessionId = null;
		if (UserContactTypeEnum.USER.getType().equals(contactType)) {
			sessionId = StringTools.getChatSessionId4User(new String[]{applyUserId, contactId});
		} else {
			sessionId = StringTools.getChatSessionId4Group(contactId);
		}

		List<ChatSessionUser> chatSessionUserList = new ArrayList<>();
		if (UserContactTypeEnum.USER.getType().equals(contactType)) {
			//创建会话
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(applyInfo);
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			//申请人session
			ChatSessionUser applySessionUser = new ChatSessionUser();
			applySessionUser.setUserId(applyUserId);
			applySessionUser.setContactId(contactId);
			applySessionUser.setSessionId(sessionId);
			UserInfo contactUser = this.userInfoMapper.selectByUserId(contactId);
			applySessionUser.setContactName(contactUser.getNickName());
			chatSessionUserList.add(applySessionUser);

			//接受人session
			ChatSessionUser contactSessionUser = new ChatSessionUser();
			contactSessionUser.setUserId(contactId);
			contactSessionUser.setContactId(applyUserId);
			contactSessionUser.setSessionId(sessionId);
			UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
			contactSessionUser.setContactName(applyUserInfo.getNickName());
			chatSessionUserList.add(contactSessionUser);
			this.chatSessionUserMapper.insertOrUpdateBatch(chatSessionUserList);

			//记录消息表
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
			chatMessage.setMessageContent(applyInfo);
			chatMessage.setSendUserId(applyUserId);
			chatMessage.setSendUserNickName(applyUserInfo.getNickName());
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(contactId);
			chatMessage.setContactType(UserContactTypeEnum.USER.getType());
			chatMessageMapper.insert(chatMessage);

			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);

			//发送给接受方申请的人
			messageHandler.sendMessage(messageSendDto);

			//发送给申请人，发送人就是接受人，联系人就是申请人
			messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
			messageSendDto.setContactId(applyUserId);
			messageSendDto.setExtendData(contactUser);
			messageHandler.sendMessage(messageSendDto);
		}
		else {
			//加入群组
			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(applyUserId);
			chatSessionUser.setContactId(contactId);
			GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
			chatSessionUser.setContactId(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insert(chatSessionUser);

			UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
			String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(), applyUserInfo.getNickName());
			//群聊会话信息
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastReceiveTime(curDate.getTime());
			chatSession.setLastMessage(sendMessage);
			this.chatSessionMapper.insertOrUpdate(chatSession);

			//增加聊天消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
			chatMessage.setMessageContent(sendMessage);
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(contactId);
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			this.chatMessageMapper.insert(chatMessage);

			//将群组添加到联系人
			redisComponent.addUserContact(applyUserId, groupInfo.getGroupId());

			//将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(applyUserId, groupInfo.getGroupId());

			//发送给被申请人
			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
			messageSendDto.setContactId(contactId);

			//查询联系人数量
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer memberCount = userContactMapper.selectCount(userContactQuery);
			messageSendDto.setMemberCount(memberCount);
			messageSendDto.setContactName(groupInfo.getGroupName());

			//发送消息给群内人员
			messageHandler.sendMessage(messageSendDto);
		}

		//TODO 创建会话
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addContact4Robot(String userId) {
		Date curDate = new Date();
		SysSettingDto sysSettingDto = redisComponent.getSysSetting();
		String contactId = sysSettingDto.getRobotUid();
		String contactName = sysSettingDto.getRobotNickName();
		String sendMessage = sysSettingDto.getRobotWelcome();
		sendMessage = StringTools.cleanHtmlTag(sendMessage);

		//增加机器人好友
		UserContact userContact = new UserContact();
		userContact.setUserId(userId);
		userContact.setContactId(contactId);
		userContact.setContactType(UserContactTypeEnum.USER.getType());
		userContact.setCreateTime(curDate);
		userContact.setLastUpdateTime(curDate);
		userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		userContactMapper.insert(userContact);

		//增加会话信息
		String sessionId = StringTools.getChatSessionId4User(new String[]{userId, contactId});
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(sendMessage);
		chatSession.setSessionId(sessionId);
		chatSession.setLastReceiveTime(curDate.getTime());
		this.chatSessionMapper.insert(chatSession);

		//增加会话信息表
		ChatSessionUser chatSessionUser = new ChatSessionUser();
		chatSessionUser.setUserId(userId);
		chatSessionUser.setContactId(contactId);
		chatSessionUser.setContactName(contactName);
		chatSessionUser.setSessionId(sessionId);
		this.chatSessionUserMapper.insert(chatSessionUser);

		//增加聊天消息
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
		chatMessage.setMessageContent(sendMessage);
		chatMessage.setSendUserId(contactId);
		chatMessage.setSendUserNickName(contactName);
		chatMessage.setSendTime(curDate.getTime());
		chatMessage.setContactId(userId);
		chatMessage.setContactType(UserContactTypeEnum.USER.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessageMapper.insert(chatMessage);
	}
}