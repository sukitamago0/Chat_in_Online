package com.easychat.entity.po;

import java.io.Serializable;

import com.easychat.enums.UserContactApplyStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @Description:联系人申请
 * @author:t1
 * @date:2025/10/22
 */
public class UserContactApply implements Serializable {
	/**
	 * 自增ID
	 */
	private Integer applyId;

	/**
	 * 申请人ID
	 */
	private String applyUserId;

	/**
	 * 接收人ID
	 */
	private String receiveUserId;

	/**
	 * 申请类型; 0:好友; 1:群组;
	 */
	private Integer contactType;

	/**
	 * 好友ID或群ID
	 */
	private String contactId;

	/**
	 * 最后申请时间
	 */
	private Long lastApplyTime;

	/**
	 * 状态; 0:待处理; 1:已同意; 2:拒绝; 3:拉黑;
	 */
//	@JsonIgnore
	private Integer status;

	/**
	 * 申请信息
	 */
	private String applyInfo;

	private String contactName;

	private String statusName;

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		return statusEnum == null ? null : statusEnum.getDesc();
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public void setApplyId(Integer applyId){
		this.applyId = applyId;
	}

	public Integer getApplyId(){
		return this.applyId;
	}

	public void setApplyUserId(String applyUserId){
		this.applyUserId = applyUserId;
	}

	public String getApplyUserId(){
		return this.applyUserId;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		return this.receiveUserId;
	}

	public void setContactType(Integer contactType){
		this.contactType = contactType;
	}

	public Integer getContactType(){
		return this.contactType;
	}

	public void setContactId(String contactId){
		this.contactId = contactId;
	}

	public String getContactId(){
		return this.contactId;
	}

	public void setLastApplyTime(Long lastApplyTime){
		this.lastApplyTime = lastApplyTime;
	}

	public Long getLastApplyTime(){
		return this.lastApplyTime;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setApplyInfo(String applyInfo){
		this.applyInfo = applyInfo;
	}

	public String getApplyInfo(){
		return this.applyInfo;
	}

	@Override
	public String toString() {
		return "自增ID:" + (applyId == null ? "null" : applyId) + ",申请人ID:" + (applyUserId == null ? "null" : applyUserId) + ",接收人ID:" + (receiveUserId == null ? "null" : receiveUserId) + ",申请类型; 0:好友; 1:群组;:" + (contactType == null ? "null" : contactType) + ",好友ID或群ID:" + (contactId == null ? "null" : contactId) + ",最后申请时间:" + (lastApplyTime == null ? "null" : lastApplyTime) + ",状态; 0:待处理; 1:已同意; 2:拒绝; 3:拉黑;:" + (status == null ? "null" : status) + ",申请信息:" + (applyInfo == null ? "null" : applyInfo);
	}
}