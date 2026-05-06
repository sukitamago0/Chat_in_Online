package com.easychat.service.Impl;

import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.enums.PageSize;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.enums.UserContactStatusEnum;
import com.easychat.enums.UserContactTypeEnum;
import com.easychat.mapper.UserContactMapper;
import com.easychat.service.ChatSessionService;
import com.easychat.websocket.MessageHandler;
import org.springframework.stereotype.Service;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.service.ChatSessionUserService;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:会话用户Service
 * @author:t1
 * @date:2025/11/21
 */

@Service("chatSessionUserService")
public class ChatSessionUserServiceImpl implements ChatSessionUserService{

	@Resource
	private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private MessageHandler messageHandler;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<ChatSessionUser> findListByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatSessionUser> list = this.findListByParam(query);
		PaginationResultVO<ChatSessionUser> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(ChatSessionUser bean) {
		return this.chatSessionUserMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<ChatSessionUser> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<ChatSessionUser> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据UserIdAndContactId查询
	 */
	public ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId,String contactId) {
		return this.chatSessionUserMapper.selectByUserIdAndContactId(userId,contactId);
	}

	/**
	 * 根据UserIdAndContactId更新
	 */
	public Integer updateChatSessionUserByUserIdAndContactId(ChatSessionUser bean, String userId,String contactId) {
		return this.chatSessionUserMapper.updateByUserIdAndContactId(bean,userId,contactId);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	public Integer deleteChatSessionUserByUserIdAndContactId(String userId,String contactId) {
		return this.chatSessionUserMapper.deleteByUserIdAndContactId(userId,contactId);
	}

	public void updateRedundanceInfo(String contactName, String contactId) {
		ChatSessionUser updateInfo = new ChatSessionUser();
		updateInfo.setContactName(contactName);

		ChatSessionUserQuery chatSessionUserQuery = new ChatSessionUserQuery();
		chatSessionUserQuery.setContactId(contactId);
		this.chatSessionUserMapper.updateByParam(updateInfo, chatSessionUserQuery);

		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

		if (contactTypeEnum == UserContactTypeEnum.GROUP) {
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setContactType(UserContactTypeEnum.getByPrefix(contactId).getType());
			messageSendDto.setContactId(contactId);
			messageSendDto.setExtendData(contactName);
			messageSendDto.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
			messageHandler.sendMessage(messageSendDto);
		} else {
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactType(UserContactTypeEnum.USER.getType());
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			List<UserContact> userContactList = userContactMapper.selectList(userContactQuery);
			for (UserContact userContact : userContactList) {
				MessageSendDto messageSendDto = new MessageSendDto();
				messageSendDto.setContactType(UserContactTypeEnum.getByPrefix(contactId).getType());
				messageSendDto.setContactId(userContact.getUserId());
				messageSendDto.setExtendData(contactName);
				messageSendDto.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
				messageSendDto.setSendUserId(contactId);
				messageSendDto.setSendUserNickName(contactName);
				messageHandler.sendMessage(messageSendDto);
			}
		}
	}

}