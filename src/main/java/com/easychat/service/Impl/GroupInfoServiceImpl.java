package com.easychat.service.Impl;

import com.easychat.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.*;
import com.easychat.redis.RedisComponent;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import io.netty.util.Constant;
import org.springframework.stereotype.Service;
import com.easychat.service.GroupInfoService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:Service
 * @author:t1
 * @date:2025/10/22
 */

@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService{

	@Resource
	private GroupInfoMapper<GroupInfo,GroupInfoQuery> groupInfoMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private AppConfig appConfig;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

	@Resource
	private MessageHandler messageHandler;

	@Resource
	private ChannelContextUtils channelContextUtils;

	@Resource
	private ChatSessionUserService chatSessionUserService;

	/**
	 * 根据条件查询列表
	 */
	public List<GroupInfo> findListByParam(GroupInfoQuery query) {
		return this.groupInfoMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(GroupInfoQuery query) {
		return this.groupInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<GroupInfo> list = this.findListByParam(query);
		PaginationResultVO<GroupInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(GroupInfo bean) {
		return this.groupInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<GroupInfo> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<GroupInfo> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据GroupId查询
	 */
	public GroupInfo getGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.selectByGroupId(groupId);
	}

	/**
	 * 根据GroupId更新
	 */
	public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
		return this.groupInfoMapper.updateByGroupId(bean,groupId);
	}

	/**
	 * 根据GroupId删除
	 */
	public Integer deleteGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.deleteByGroupId(groupId);
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {

		Date curDate = new Date();

		// 空则新增
		if (StringTools.isEmpty(groupInfo.getGroupId())) {
			GroupInfoQuery query = new GroupInfoQuery();
			query.setGroupOwnerId(groupInfo.getGroupOwnerId());
			Integer count = this.groupInfoMapper.selectCount(query);
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			if (count >= sysSettingDto.getMaxGroupCount()) {
				throw new BusinessException("最多能支持创建" + sysSettingDto.getMaxGroupCount() + "个群聊");
			}

			if (null == avatarFile) {
			//	暂时注释让前端可以不传头像
			//	throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			groupInfo.setCreateTime(curDate);
			groupInfo.setGroupId(StringTools.getGroupId());
			this.groupInfoMapper.insert(groupInfo);

			//将群组添加为联系人
			UserContact userContact = new UserContact();
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setContactType(UserContactTypeEnum.GROUP.getType());
			userContact.setContactId(groupInfo.getGroupId());
			userContact.setUserId(groupInfo.getGroupOwnerId());
			userContact.setCreateTime(curDate);
			this.userContactMapper.insert(userContact);

			//创建会话
			String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
			chatSessionUser.setContactId(groupInfo.getGroupId());
			chatSessionUser.setContactName(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insert(chatSessionUser);

			//创建消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
			chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(groupInfo.getGroupId());
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			chatMessageMapper.insert(chatMessage);

			//将群组添加到联系人
			redisComponent.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

			//将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

			//发送ws消息
			chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSessionUser.setLastReceiveTime(curDate.getTime());
			chatSessionUser.setMemberCount(1);

			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
			messageSendDto.setExtendData(chatSessionUser);
			messageSendDto.setLastMessage(chatSessionUser.getLastMessage());
			messageHandler.sendMessage(messageSendDto);
			//TODO 发送消息
		} else {
			GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
			if(!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())) {
				//对比数据库与前端传来的群id所对应的群的群主id 防止非群主修改群数据
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			this.groupInfoMapper.updateByGroupId(groupInfo,groupInfo.getGroupId());
			//更新相关表的冗余信息
			String contactNameUpdate = null;
			if (!StringTools.isEmpty(groupInfo.getGroupName())) {
				contactNameUpdate = groupInfo.getGroupName();
			}
			if (contactNameUpdate == null) {
				return;
			}

			chatSessionUserService.updateRedundanceInfo(contactNameUpdate,groupInfo.getGroupId());

			//TODO 修改群名称后发送ws消息
		}
		if (null == avatarFile) {
			return ;
		}
		String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
		File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
		if(!targetFileFolder.exists()){
			targetFileFolder.mkdir();
		}
		String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Constants.IMAGE_SUFFIX;
 		avatarFile.transferTo(new File(filePath));
		avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
	}
}