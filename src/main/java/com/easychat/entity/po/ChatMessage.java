package com.easychat.entity.po;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @Description:聊天消息表
 * @author:t1
 * @date:2025/11/21
 */
public class ChatMessage implements Serializable {
	/**
	 * 消息自增id
	 */
	private Long messageId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 消息内容
	 */
	private String messageContent;

	/**
	 * 发送人ID
	 */
	private String sendUserId;

	/**
	 * 发送人昵称
	 */
	private String sendUserNickName;

	/**
	 * 发送时间
	 */
	private Long sendTime;

	/**
	 * 接收联系人ID
	 */
	private String contactId;

	/**
	 * 联系人类型 0:好友 1:群聊
	 */
	private Integer contactType;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 文件类型
	 */
	private Integer fileType;

	/**
	 * 状态 0:已发送 1:已读
	 */
	@JsonIgnore
	private Integer status;


	private Integer messageType;

	public Integer getMessageType() {
		return messageType;
	}

	public void setMessageType(Integer messageType) {
		this.messageType = messageType;
	}

	public void setMessageId(Long messageId){
		this.messageId = messageId;
	}

	public Long getMessageId(){
		return this.messageId;
	}

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public String getSessionId(){
		return this.sessionId;
	}

	public void setMessageContent(String messageContent){
		this.messageContent = messageContent;
	}

	public String getMessageContent(){
		return this.messageContent;
	}

	public void setSendUserId(String sendUserId){
		this.sendUserId = sendUserId;
	}

	public String getSendUserId(){
		return this.sendUserId;
	}

	public void setSendUserNickName(String sendUserNickName){
		this.sendUserNickName = sendUserNickName;
	}

	public String getSendUserNickName(){
		return this.sendUserNickName;
	}

	public void setSendTime(Long sendTime){
		this.sendTime = sendTime;
	}

	public Long getSendTime(){
		return this.sendTime;
	}

	public void setContactId(String contactId){
		this.contactId = contactId;
	}

	public String getContactId(){
		return this.contactId;
	}

	public void setContactType(Integer contactType){
		this.contactType = contactType;
	}

	public Integer getContactType(){
		return this.contactType;
	}

	public void setFileSize(Long fileSize){
		this.fileSize = fileSize;
	}

	public Long getFileSize(){
		return this.fileSize;
	}

	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public String getFileName(){
		return this.fileName;
	}

	public void setFileType(Integer fileType){
		this.fileType = fileType;
	}

	public Integer getFileType(){
		return this.fileType;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	@Override
	public String toString() {
		return "消息自增id:" + (messageId == null ? "null" : messageId) + ",会话ID:" + (sessionId == null ? "null" : sessionId) + ",消息内容:" + (messageContent == null ? "null" : messageContent) + ",发送人ID:" + (sendUserId == null ? "null" : sendUserId) + ",发送人昵称:" + (sendUserNickName == null ? "null" : sendUserNickName) + ",发送时间:" + (sendTime == null ? "null" : sendTime) + ",接收联系人ID:" + (contactId == null ? "null" : contactId) + ",联系人类型 0:好友 1:群聊:" + (contactType == null ? "null" : contactType) + ",文件大小:" + (fileSize == null ? "null" : fileSize) + ",文件名:" + (fileName == null ? "null" : fileName) + ",文件类型:" + (fileType == null ? "null" : fileType) + ",状态 0:已发送 1:已读:" + (status == null ? "null" : status) + "message_type:"+(messageType == null ? "null" : messageType);
	}
}