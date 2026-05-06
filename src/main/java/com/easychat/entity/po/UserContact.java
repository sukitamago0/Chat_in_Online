package com.easychat.entity.po;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import com.easychat.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @Description:联系人
 * @author:t1
 * @date:2025/10/22
 */
public class UserContact implements Serializable {
	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 联系人ID或群聊ID
	 */
	private String contactId;

	/**
	 * 联系人类型; 0:好友; 1:群组;
	 */
	private Integer contactType;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date createTime;

	/**
	 * 状态; 0:非好友; 1:好友; 2:已删除好友; 3:被好友删除；4:已拉黑好友；5:被好友拉黑
	 */
	@JsonIgnore
	private Integer status;

	/**
	 * 最后更新时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date lastUpdateTime;

	//非数据库字段 联系人名称
	private String contactName;

	//非数据库字段 联系人性别
	private Integer sex;

	public Integer getSex() {
		return sex;
	}

	public void setSex(Integer sex) {
		this.sex = sex;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
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

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setLastUpdateTime(Date lastUpdateTime){
		this.lastUpdateTime = lastUpdateTime;
	}

	public Date getLastUpdateTime(){
		return this.lastUpdateTime;
	}

	@Override
	public String toString() {
		return "用户ID:" + (userId == null ? "null" : userId) + ",联系人ID或群聊ID:" + (contactId == null ? "null" : contactId) + ",联系人类型; 0:好友; 1:群组;:" + (contactType == null ? "null" : contactType) + ",创建时间:" + (createTime == null ? "null" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",状态; 0:非好友; 1:好友; 2:已删除好友; 3:被好友删除；4:已拉黑好友；5:被好友拉黑:" + (status == null ? "null" : status) + ",最后更新时间:" + (lastUpdateTime == null ? "null" : DateUtil.format(lastUpdateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}