package com.easychat.service.Impl;

import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserContactApply;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.UserContactMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.service.UserContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.service.UserContactApplyService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:联系人申请Service
 * @author:t1
 * @date:2025/10/22
 */

@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService{

	private static final Logger log = LoggerFactory.getLogger(UserContactApplyServiceImpl.class);
	@Resource
	private UserContactApplyMapper<UserContactApply,UserContactApplyQuery> userContactApplyMapper;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private UserContactService userContactService;

	/**
	 * 根据条件查询列表
	 */
	public List<UserContactApply> findListByParam(UserContactApplyQuery query) {
		return this.userContactApplyMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserContactApplyQuery query) {
		return this.userContactApplyMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserContactApply> list = this.findListByParam(query);
//		log.info(list.toString());
		PaginationResultVO<UserContactApply> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(UserContactApply bean) {
		return this.userContactApplyMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserContactApply> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<UserContactApply> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据ApplyId查询
	 */
	public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.selectByApplyId(applyId);
	}

	/**
	 * 根据ApplyId更新
	 */
	public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
		return this.userContactApplyMapper.updateByApplyId(bean,applyId);
	}

	/**
	 * 根据ApplyId删除
	 */
	public Integer deleteUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.deleteByApplyId(applyId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId查询
	 */
	public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId,String receiveUserId,String contactId) {
		return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId,receiveUserId,contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId更新
	 */
	public Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId,String receiveUserId,String contactId) {
		return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean,applyUserId,receiveUserId,contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId删除
	 */
	public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId,String receiveUserId,String contactId) {
		return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId,receiveUserId,contactId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dealWithApply(String userId, Integer applyId, Integer status) {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		if(statusEnum == null || UserContactApplyStatusEnum.INIT == statusEnum) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserContactApply applyInfo = this.userContactApplyMapper.selectByApplyId(applyId);
		if (applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())) {//防止非接收者处理消息
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserContactApply updateInfo = new UserContactApply();
		updateInfo.setStatus(statusEnum.getStatus());
		updateInfo.setLastApplyTime(System.currentTimeMillis());

		UserContactApplyQuery applyQuery = new UserContactApplyQuery();
		applyQuery.setApplyId(applyId);
		applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());

		Integer count = userContactApplyMapper.updateByParam(updateInfo,applyQuery);//update {updateInfo} where {applyQuery}

		if (count == 0) {//count:更新的记录数为0则抛异常
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		//通过请求
		if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)){
			//添加联系人
			userContactService.addContact(applyInfo.getApplyUserId(), applyInfo.getReceiveUserId(), applyInfo.getContactId(), applyInfo.getContactType(), applyInfo.getApplyInfo());
			return ;
		}

		//若被拉黑
		if (UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)){
			Date curDate = new Date();
			UserContact userContact = new UserContact();
			userContact.setUserId(applyInfo.getApplyUserId());
			userContact.setContactId(applyInfo.getContactId());
			userContact.setContactType(applyInfo.getContactType());
			userContact.setCreateTime(curDate);
			userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus());
			userContact.setLastUpdateTime(curDate);
			userContactMapper.insertOrUpdate(userContact);
		}
	}


}