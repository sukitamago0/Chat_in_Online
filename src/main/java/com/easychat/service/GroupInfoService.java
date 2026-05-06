package com.easychat.service;

import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.GroupInfo;
import com.easychat.entity.query.GroupInfoQuery;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
/**
 * @Description:Service
 * @author:t1
 * @date:2025/10/22
 */
public interface GroupInfoService{

	/**
	 * 根据条件查询列表
	 */
	List<GroupInfo> findListByParam(GroupInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(GroupInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(GroupInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<GroupInfo> listbean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<GroupInfo> listbean);

	/**
	 * 根据GroupId查询
	 */
	GroupInfo getGroupInfoByGroupId(String groupId);

	/**
	 * 根据GroupId更新
	 */
	Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId);

	/**
	 * 根据GroupId删除
	 */
	Integer deleteGroupInfoByGroupId(String groupId);

	void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;
}