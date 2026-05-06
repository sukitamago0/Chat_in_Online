package com.easychat.service.Impl;

import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserInfoQuery;
import com.easychat.enums.BeautyAccountStatusEnum;
import com.easychat.enums.PageSize;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.UserInfoBeautyQuery;
import com.easychat.enums.ResponseCodeEnum;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.UserInfoMapper;
import org.springframework.stereotype.Service;
import com.easychat.mapper.UserInfoBeautyMapper;
import com.easychat.service.UserInfoBeautyService;
import java.util.List;
import javax.annotation.Resource;

/**
 * @Description:靓号表
Service
 * @author:t1
 * @date:2025/10/16
 */

@Service("userInfoBeautyService")
public class UserInfoBeautyServiceImpl implements UserInfoBeautyService{

	@Resource
	private UserInfoBeautyMapper<UserInfoBeauty,UserInfoBeautyQuery> userInfoBeautyMapper;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<UserInfoBeauty> findListByParam(UserInfoBeautyQuery query) {
		return this.userInfoBeautyMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserInfoBeautyQuery query) {
		return this.userInfoBeautyMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserInfoBeauty> findListByPage(UserInfoBeautyQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserInfoBeauty> list = this.findListByParam(query);
		PaginationResultVO<UserInfoBeauty> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(UserInfoBeauty bean) {
		return this.userInfoBeautyMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserInfoBeauty> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userInfoBeautyMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<UserInfoBeauty> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userInfoBeautyMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据IdAndUserId查询
	 */
	public UserInfoBeauty getUserInfoBeautyByIdAndUserId(Integer id,String userId) {
		return this.userInfoBeautyMapper.selectByIdAndUserId(id,userId);
	}

	/**
	 * 根据IdAndUserId更新
	 */
	public Integer updateUserInfoBeautyByIdAndUserId(UserInfoBeauty bean, Integer id,String userId) {
		return this.userInfoBeautyMapper.updateByIdAndUserId(bean,id,userId);
	}

	/**
	 * 根据IdAndUserId删除
	 */
	public Integer deleteUserInfoBeautyByIdAndUserId(Integer id,String userId) {
		return this.userInfoBeautyMapper.deleteByIdAndUserId(id,userId);
	}

	/**
	 * 根据UserId查询
	 */
	public UserInfoBeauty getUserInfoBeautyByUserId(String userId) {
		return this.userInfoBeautyMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId更新
	 */
	public Integer updateUserInfoBeautyByUserId(UserInfoBeauty bean, String userId) {
		return this.userInfoBeautyMapper.updateByUserId(bean,userId);
	}

	/**
	 * 根据UserId删除
	 */
	public Integer deleteUserInfoBeautyByUserId(String userId) {
		return this.userInfoBeautyMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email查询
	 */
	public UserInfoBeauty getUserInfoBeautyByEmail(String email) {
		return this.userInfoBeautyMapper.selectByEmail(email);
	}

	/**
	 * 根据Email更新
	 */
	public Integer updateUserInfoBeautyByEmail(UserInfoBeauty bean, String email) {
		return this.userInfoBeautyMapper.updateByEmail(bean,email);
	}

	/**
	 * 根据Email删除
	 */
	public Integer deleteUserInfoBeautyByEmail(String email) {
		return this.userInfoBeautyMapper.deleteByEmail(email);
	}

	@Override
	public void saveAccount(UserInfoBeauty beauty) {
		if (beauty.getId() != null) {
			UserInfoBeauty dbInfo = this.userInfoBeautyMapper.selectById(beauty.getId());
			if (BeautyAccountStatusEnum.USED.getStatus().equals(dbInfo.getStatus())) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		//添加时邮箱判断是否存在
		UserInfoBeauty dbInfo = this.userInfoBeautyMapper.selectByEmail(beauty.getEmail());
		if (beauty.getId() == null && dbInfo != null) {
			throw new BusinessException("靓号邮箱已经存在");
		}

		//修改时判断邮箱是否存在
		if (beauty.getId() != null && dbInfo != null && dbInfo.getId() != null && !beauty.getId().equals(dbInfo.getId())) {
			throw new BusinessException("靓号邮箱已经存在");
		}

		//判断账号是否存在
		dbInfo = this.userInfoBeautyMapper.selectByUserId(beauty.getUserId());
		if (beauty.getId() == null && dbInfo != null) {
			throw new BusinessException("账号已经存在");
		}

		//修改时判断账号是否存在
		if (beauty.getId() != null && dbInfo != null && dbInfo.getId() != null && !beauty.getId().equals(dbInfo.getId())) {
			throw new BusinessException("账号已经存在");
		}

		//判断邮箱是否已经注册
		UserInfo userInfo = this.userInfoMapper.selectByEmail(beauty.getEmail());
		if (userInfo != null) {
			throw new BusinessException("靓号邮箱已经注册");
		}

		userInfo = this.userInfoMapper.selectByUserId(beauty.getUserId());
		if (userInfo != null) {
			throw new BusinessException("靓号已经被注册");
		}

		if (beauty.getId() != null) {
			this.userInfoBeautyMapper.updateById(beauty, beauty.getId());
		} else {
			beauty.setStatus(BeautyAccountStatusEnum.NO_USE.getStatus());
			this.userInfoBeautyMapper.insert(beauty);
		}
	}

	@Override
	public void deleteBeautyById(Integer id) {
		this.userInfoBeautyMapper.deleteById(id);
	}

}