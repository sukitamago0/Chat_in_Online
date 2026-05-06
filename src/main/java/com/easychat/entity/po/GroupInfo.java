package com.easychat.entity.po;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import com.easychat.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @Description:
 * @author:t1
 * @date:2025/10/22
 */
public class GroupInfo implements Serializable {
	/**
	 * 群ID
	 */
	private String groupId;

	/**
	 * 群组名
	 */
	private String groupName;

	/**
	 * 群主id
	 */
	private String groupOwnerId;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date createTime;

	/**
	 * 群公告
	 */
	private String groupNotice;

	/**
	 * 0:直接加入; 1:管理员同意后加入
	 */
	private Integer joinType;

	/**
	 * 状态: 0禁用; 1启用;
	 */
	@JsonIgnore
	private Integer status;

	//非数据库字段 成员数
	private Integer memberCount;

	public Integer getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(Integer memberCount) {
		this.memberCount = memberCount;
	}

	public void setGroupId(String groupId){
		this.groupId = groupId;
	}

	public String getGroupId(){
		return this.groupId;
	}

	public void setGroupName(String groupName){
		this.groupName = groupName;
	}

	public String getGroupName(){
		return this.groupName;
	}

	public void setGroupOwnerId(String groupOwnerId){
		this.groupOwnerId = groupOwnerId;
	}

	public String getGroupOwnerId(){
		return this.groupOwnerId;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setGroupNotice(String groupNotice){
		this.groupNotice = groupNotice;
	}

	public String getGroupNotice(){
		return this.groupNotice;
	}

	public void setJoinType(Integer joinType){
		this.joinType = joinType;
	}

	public Integer getJoinType(){
		return this.joinType;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	@Override
	public String toString() {
		return "群ID:" + (groupId == null ? "null" : groupId) + ",群组名:" + (groupName == null ? "null" : groupName) + ",群主id:" + (groupOwnerId == null ? "null" : groupOwnerId) + ",创建时间:" + (createTime == null ? "null" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",群公告:" + (groupNotice == null ? "null" : groupNotice) + ",0:直接加入; 1:管理员同意后加入:" + (joinType == null ? "null" : joinType) + ",状态: 0禁用; 1启用;:" + (status == null ? "null" : status);
	}
}