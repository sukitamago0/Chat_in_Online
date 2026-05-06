package com.easychat.service;

import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.enums.UserContactStatusEnum;

import java.util.List;
/**
 * @Description:联系人Service
 * @author:t1
 * @date:2025/10/22
 */
public interface UserContactService{

	/**
	 * 根据条件查询列表
	 */
	List<UserContact> findListByParam(UserContactQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserContactQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserContact> findListByPage(UserContactQuery query);

	/**
	 * 新增
	 */
	Integer add(UserContact bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserContact> listbean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<UserContact> listbean);

	/**
	 * 根据UserIdAndContactId查询
	 */
	UserContact getUserContactByUserIdAndContactId(String userId,String contactId);

	/**
	 * 根据UserIdAndContactId更新
	 */
	Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId,String contactId);

	/**
	 * 根据UserIdAndContactId删除
	 */
	Integer deleteUserContactByUserIdAndContactId(String userId,String contactId);

	UserContactSearchResultDto searchContact(String userId, String contactId);

	Integer applyAdd(TokenUserInfoDto tokenUserInfoDto, String contactId, String applyMsg);

	void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum);

	void addContact(String applyUserId, String receriveUserId, String contactId, Integer contactType, String applyInfo);

	void addContact4Robot(String UserId);

}