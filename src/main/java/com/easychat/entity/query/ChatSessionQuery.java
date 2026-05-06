package com.easychat.entity.query;



/**
 * @Description:会话信息查询对象
 * @author:t1
 * @date:2025/11/21
 */
public class ChatSessionQuery extends BaseQuery{
	/**
	 * 会话ID
	 */
	private String sessionId;

	private String sessionIdFuzzy;

	/**
	 * 最后接收的消息
	 */
	private String lastMessage;

	private String lastMessageFuzzy;

	/**
	 * 最后接受消息的时间
	 */
	private Long lastReceiveTime;

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public String getSessionId(){
		return this.sessionId;
	}

	public void setLastMessage(String lastMessage){
		this.lastMessage = lastMessage;
	}

	public String getLastMessage(){
		return this.lastMessage;
	}

	public void setLastReceiveTime(Long lastReceiveTime){
		this.lastReceiveTime = lastReceiveTime;
	}

	public Long getLastReceiveTime(){
		return this.lastReceiveTime;
	}

	public void setSessionIdFuzzy(String sessionIdFuzzy){
		this.sessionIdFuzzy = sessionIdFuzzy;
	}

	public String getSessionIdFuzzy(){
		return this.sessionIdFuzzy;
	}

	public void setLastMessageFuzzy(String lastMessageFuzzy){
		this.lastMessageFuzzy = lastMessageFuzzy;
	}

	public String getLastMessageFuzzy(){
		return this.lastMessageFuzzy;
	}

}