package com.easychat.service.Impl;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.ChatSession;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.query.ChatSessionQuery;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.entity.query.SimplePage;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.query.ChatMessageQuery;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.service.ChatMessageService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:聊天消息表Service
 * @author:t1
 * @date:2025/11/21
 */

@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService{

	@Resource
	private ChatMessageMapper<ChatMessage,ChatMessageQuery> chatMessageMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	public List<ChatMessage> findListByParam(ChatMessageQuery query) {
		return this.chatMessageMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(ChatMessageQuery query) {
		return this.chatMessageMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(query);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<ChatMessage> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<ChatMessage> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据MessageId查询
	 */
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId更新
	 */
	public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean,messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}



//	***
	@Override
	public MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) {
		//不是机器人回复，判断联系人状态
		if (!Constants.ROBOT_UID.equals(tokenUserInfoDto.getUserId())) {
			List<String> contactList = redisComponent.getUserContactList(tokenUserInfoDto.getUserId());
			if (!contactList.contains(chatMessage.getContactId())) {
				UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
				if (UserContactTypeEnum.USER == userContactTypeEnum) {
					throw new BusinessException(ResponseCodeEnum.CODE_902);
				} else {
					throw new BusinessException(ResponseCodeEnum.CODE_903);
				}
			}
		}

		//tokenUserInfoDto 是发送者
		String sessionId = null;
		String sendUserId = tokenUserInfoDto.getUserId();
		String contactId = chatMessage.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

		if(UserContactTypeEnum.USER == contactTypeEnum) {
			sessionId = StringTools.getChatSessionId4User(new String[] {sendUserId,contactId});
		}else{
			sessionId = StringTools.getChatSessionId4Group(contactId);
		}
		chatMessage.setSessionId(sessionId);

		Long curTime = System.currentTimeMillis();
		chatMessage.setSendTime(curTime);

		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
		if (null == messageTypeEnum || !ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(), MessageTypeEnum.MEDIA_CHAT.getType()}, chatMessage.getMessageType())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer status = MessageTypeEnum.MEDIA_CHAT==messageTypeEnum ? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();

		String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());
		chatMessage.setMessageContent(messageContent);

		//更新会话 保存最后一条消息信息
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		if (UserContactTypeEnum.GROUP==contactTypeEnum) {
			chatSession.setLastMessage(tokenUserInfoDto.getNickName() + ": " + messageContent);
		}
		chatSession.setLastReceiveTime(curTime);
		chatSessionMapper.updateBySessionId(chatSession, sessionId);

		//记录消息表
		chatMessage.setSendUserId(sendUserId);
		chatMessage.setSendUserNickName(tokenUserInfoDto.getNickName());
		chatMessage.setContactType(contactTypeEnum.getType());
		chatMessageMapper.insert(chatMessage);

		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);

		if (Constants.ROBOT_UID.equals(contactId)) {//如果是机器人则调用特殊接口返回
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			TokenUserInfoDto robot = new TokenUserInfoDto();
			robot.setUserId(sysSettingDto.getRobotUid());
			robot.setNickName(sysSettingDto.getRobotNickName());
			ChatMessage robotChatMessage = new ChatMessage();
			robotChatMessage.setContactId(sendUserId);
			//这里可以补充 ai 回复聊天
			robotChatMessage.setMessageContent("我只是一个机器人");
			robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
			saveMessage(robotChatMessage, robot);
		} else {
			messageHandler.sendMessage(messageSendDto);//发送消息
		}

		return messageSendDto;

	}

	@Override
	public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {
		ChatMessage chatMessage = chatMessageMapper.selectByMessageId(messageId);
		//防止越过前端
		if (chatMessage == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (!chatMessage.getSendUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		//大小异常 或文件类别不支持
		SysSettingDto sysSettingDto = redisComponent.getSysSetting();
		String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());
		if (!StringTools.isEmpty(fileSuffix)
					&& ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toUpperCase())
					&& file.getSize() > sysSettingDto.getMaxImageSize() * Constants.FILE_SIZE_MB) {
					throw new BusinessException(ResponseCodeEnum.CODE_600);
		}else if (ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toUpperCase())
					&&file.getSize() > sysSettingDto.getMaxVideoSize() * Constants.FILE_SIZE_MB) {
					throw new BusinessException(ResponseCodeEnum.CODE_600);
		} else if (!StringTools.isEmpty(fileSuffix)
					&&ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toUpperCase())
					&&ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toUpperCase())
					&&file.getSize() > sysSettingDto.getMaxFileSize() * Constants.FILE_SIZE_MB) {
					throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

	}
}