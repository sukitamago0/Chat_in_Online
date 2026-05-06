package com.easychat.service;

import com.easychat.entity.query.UserInfoQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.UserInfoBeautyQuery;
import java.util.List;
/**
 * @Description:靓号表
Service
 * @author:t1
 * @date:2025/10/16
 */
public interface UserInfoBeautyService{

	/**
	 * 根据条件查询列表
	 */
	List<UserInfoBeauty> findListByParam(UserInfoBeautyQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserInfoBeautyQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfoBeauty> findListByPage(UserInfoBeautyQuery query);

	/**
	 * 新增
	 */
	Integer add(UserInfoBeauty bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfoBeauty> listbean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<UserInfoBeauty> listbean);

	/**
	 * 根据IdAndUserId查询
	 */
	UserInfoBeauty getUserInfoBeautyByIdAndUserId(Integer id,String userId);

	/**
	 * 根据IdAndUserId更新
	 */
	Integer updateUserInfoBeautyByIdAndUserId(UserInfoBeauty bean, Integer id,String userId);

	/**
	 * 根据IdAndUserId删除
	 */
	Integer deleteUserInfoBeautyByIdAndUserId(Integer id,String userId);

	/**
	 * 根据UserId查询
	 */
	UserInfoBeauty getUserInfoBeautyByUserId(String userId);

	/**
	 * 根据UserId更新
	 */
	Integer updateUserInfoBeautyByUserId(UserInfoBeauty bean, String userId);

	/**
	 * 根据UserId删除
	 */
	Integer deleteUserInfoBeautyByUserId(String userId);

	/**
	 * 根据Email查询
	 */
	UserInfoBeauty getUserInfoBeautyByEmail(String email);

	/**
	 * 根据Email更新
	 */
	Integer updateUserInfoBeautyByEmail(UserInfoBeauty bean, String email);

	/**
	 * 根据Email删除
	 */
	Integer deleteUserInfoBeautyByEmail(String email);

	void saveAccount(UserInfoBeauty beauty);

	void deleteBeautyById(Integer id);
}