package com.easychat.mapper;

import org.apache.ibatis.annotations.Param;
/**
 * @Description:用户信息Mapper
 * @author:t1
 * @date:2025/10/16
 */
public interface UserInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserId查询
	 */
	T selectByUserId(@Param("userId") String userId);

	/**
	 * 根据UserId更新
	 */
	Integer updateByUserId(@Param("bean") T t, @Param("userId") String userId);

	/**
	 * 根据UserId删除
	 */
	Integer deleteByUserId(@Param("userId") String userId);

	/**
	 * 根据Email查询
	 */
	T selectByEmail(@Param("email") String email);

	/**
	 * 根据Email更新
	 */
	Integer updateByEmail(@Param("bean") T t, @Param("email") String email);

	/**
	 * 根据Email删除
	 */
	Integer deleteByEmail(@Param("email") String email);


}