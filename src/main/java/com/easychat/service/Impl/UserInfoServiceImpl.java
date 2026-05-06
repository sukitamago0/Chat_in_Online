package com.easychat.service.Impl;

import com.easychat.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.enums.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.UserInfoQuery;
import com.easychat.exception.BusinessException;
import com.easychat.mapper.UserContactMapper;
import com.easychat.mapper.UserInfoBeautyMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.redis.RedisUtils;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.UserContactService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import org.apache.catalina.User;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.UserInfoService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * @Description:用户信息Service
 * @author:t1
 * @date:2025/10/16
 */

@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService{

	@Resource
	private UserInfoMapper<UserInfo,UserInfoQuery> userInfoMapper;

	@Resource
	private UserInfoBeautyMapper<UserInfoBeauty,UserInfoQuery> userInfoBeautyMapper;

	@Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;

	@Resource
	private AppConfig appConfig;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private UserContactService userContactService;

	@Resource
	private ChatSessionUserService chatSessionUserService;
    @Autowired
    private RedisUtils redisUtils;

	/**
	 * 根据条件查询列表
	 */
	public List<UserInfo> findListByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(query);
		PaginationResultVO<UserInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageTotal(), list);
		return result;	}

	/**
	 * 新增
	 */
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserInfo> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listbean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<UserInfo> listbean) {
		if (listbean == null || listbean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listbean);
	}

	/**
	 * 根据UserId查询
	 */
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId更新
	 */
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean,userId);
	}

	/**
	 * 根据UserId删除
	 */
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email查询
	 */
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email更新
	 */
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean,email);
	}

	/**
	 * 根据Email删除
	 */
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(String email, String nickName, String password) {
		//该方法同时操作两个表 故需要事务处理@Transactional

		Map<String, Object> result = new HashMap<>();
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);

		if(null != userInfo){
			throw new BusinessException("邮箱已经存在");
		}

			String userId = StringTools.getUserId();//生成随机UserId

			//靓号表中存在靓号且未使用
			UserInfoBeauty beautyAccount = this.userInfoBeautyMapper.selectByEmail(email);
			Boolean useBeautyAccount = null != beautyAccount && BeautyAccountStatusEnum.NO_USE.getStatus().equals(beautyAccount.getStatus());
			if (useBeautyAccount){
				userId = UserContactTypeEnum.USER.getPrefix() + beautyAccount.getUserId();
			}

			Date curDate = new Date();
			userInfo = new UserInfo();
			userInfo.setUserId(userId);
			userInfo.setEmail(email);
			userInfo.setNickName(nickName);
			userInfo.setPassword(StringTools.encodeMd5(password));//controller层已经校验过非空，一般而言此处可以再校验密码规则
			userInfo.setCreateTime(curDate);
			userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
			userInfo.setLastOffTime(curDate.getTime());
			userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
			this.userInfoMapper.insert(userInfo);

			//如果该账号是靓号
			if (useBeautyAccount){
				UserInfoBeauty updateBeauty = new UserInfoBeauty();
				updateBeauty.setStatus(BeautyAccountStatusEnum.USED.getStatus());
				this.userInfoBeautyMapper.updateByUserId(updateBeauty,beautyAccount.getUserId());
			}

			//TODO 创建机器人好友
//			userContactService.addContact4Robot(userInfo.getUserId());

	}


	@Override
	public UserInfoVO login(String email, String password) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);

		if(null == userInfo || !userInfo.getPassword().equals(StringTools.encodeMd5(password))){
			throw new BusinessException("账号或密码不存在");
		}

		if(UserStatusEnum.DISABLE.equals(userInfo.getStatus())){
			throw new BusinessException("账号已禁用");
		}

		//查询联系人
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(userInfo.getUserId());
		contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> contactList = userContactMapper.selectList(contactQuery);

		List<String> contactIdList = contactList.stream().map(item->item.getContactId()).collect(Collectors.toList());
		redisComponent.cleanUserContact(userInfo.getUserId());
		if(!contactIdList.isEmpty()){
			redisComponent.addUserContactBatch(userInfo.getUserId(),contactIdList);
		}

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(userInfo);

		Long lastHeartBeat = redisComponent.getUserHeartBeat(userInfo.getUserId());
		if(null != lastHeartBeat){
			throw new BusinessException("账号已登录,请退出后再登录");
		}

		//保存登录信息到redis
		String token = StringTools.encodeMd5(tokenUserInfoDto.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
		tokenUserInfoDto.setToken(token);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);

		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setToken(tokenUserInfoDto.getToken());
		userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());

		return userInfoVO;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		if (avatarFile != null) {
			String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
			File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
			if (!targetFileFolder.exists()) {
				//若无该目录则创建
				targetFileFolder.mkdirs();
			}
			//使用相同路径在新上传头像时覆盖旧头像 且使用userid作为路径则不用保存数据库
			String filePath = targetFileFolder.getPath() + "/" + userInfo.getUserId() + Constants.IMAGE_SUFFIX;
			avatarFile.transferTo(new File(filePath));
			avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
		}

		UserInfo dbInfo = this.userInfoMapper.selectByUserId(userInfo.getUserId());//查询不是事务操作 先查询可以让后边的事务锁时间更短

		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
		String contactNameUpdate = null;
		//如果用户名被更改则要修改昵称信息
		if (dbInfo.getNickName().equals(userInfo.getNickName())) {
			contactNameUpdate = userInfo.getNickName();
		}
		if (contactNameUpdate != null) {
			return;
		}

		TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDtoByUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(contactNameUpdate);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);

		chatSessionUserService.updateRedundanceInfo(contactNameUpdate,userInfo.getUserId());
	}

	@Override
	public void updateUserStatus(Integer status, String userId) {
		//若传入未定义的枚举类型则报错
		UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
		if (userStatusEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserInfo userInfo = new UserInfo();
		userInfo.setStatus(userStatusEnum.getStatus());
		this.userInfoMapper.updateByUserId(userInfo, userId);
	}

	@Override
	public void forceOffLine(String userId) {
		//TODO 强制下线
	}

	private TokenUserInfoDto getTokenUserInfoDto(UserInfo userInfo){
		TokenUserInfoDto tokenUserInfoDto = new TokenUserInfoDto();

		tokenUserInfoDto.setUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(userInfo.getNickName());

		String adminEmails = appConfig.getAdminEmails();
		if(!StringTools.isEmpty(adminEmails) && ArrayUtils.contains(adminEmails.split(","), userInfo.getEmail())){
			tokenUserInfoDto.setAdmin(true);
		} else {
			tokenUserInfoDto.setAdmin(false);
		}
		return tokenUserInfoDto;
	}


}