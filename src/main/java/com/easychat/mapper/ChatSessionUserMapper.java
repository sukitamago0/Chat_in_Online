package com.easychat.mapper;

import org.apache.ibatis.annotations.Param;
/**
 * @Description:会话用户Mapper
 * @author:t1
 * @date:2025/11/21
 */
public interface ChatSessionUserMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserIdAndContactId查询
	 */
	T selectByUserIdAndContactId(@Param("userId") String userId,@Param("contactId") String contactId);

	/**
	 * 根据UserIdAndContactId更新
	 */
	Integer updateByUserIdAndContactId(@Param("bean") T t, @Param("userId") String userId,@Param("contactId") String contactId);

	/**
	 * 根据UserIdAndContactId删除
	 */
	Integer deleteByUserIdAndContactId(@Param("userId") String userId,@Param("contactId") String contactId);

	/**
	 * 根据条件更新
	 */
	Integer updateByParam(
			@Param("bean") T bean,
			@Param("query") P query
	);


}