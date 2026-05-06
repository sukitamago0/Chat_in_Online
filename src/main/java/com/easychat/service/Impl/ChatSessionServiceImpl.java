package com.easychat.service.Impl;

import com.easychat.entity.query.SimplePage;
import com.easychat.enums.PageSize;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.ChatSession;
import com.easychat.entity.query.ChatSessionQuery;
import org.springframework.stereotype.Service;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.service.ChatSessionService;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:会话信息Service
 * @author:t1
 * @date:2025/11/21
 */

@Service("chatSessionService")
public class ChatSessionServiceImpl implements ChatSessionService{

	@Resource
	private ChatSessionMapper<ChatSession,ChatSessionQuery> chatSessionMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<ChatSession> findListByParam(ChatSessionQuery query) {
		return this.chatSessionMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(ChatSessionQuery query) {
		return this.chatSessionMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<ChatSession> findListByPage(ChatSessionQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatSession> list = this.findListByParam(query);
		PaginationResultVO<ChatSession> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(ChatSession bean) {
		return this.chatSessionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<ChatSession> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatSessionMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<ChatSession> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.chatSessionMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据SessionId查询
	 */
	public ChatSession getChatSessionBySessionId(String sessionId) {
		return this.chatSessionMapper.selectBySessionId(sessionId);
	}

	/**
	 * 根据SessionId更新
	 */
	public Integer updateChatSessionBySessionId(ChatSession bean, String sessionId) {
		return this.chatSessionMapper.updateBySessionId(bean,sessionId);
	}

	/**
	 * 根据SessionId删除
	 */
	public Integer deleteChatSessionBySessionId(String sessionId) {
		return this.chatSessionMapper.deleteBySessionId(sessionId);
	}

}