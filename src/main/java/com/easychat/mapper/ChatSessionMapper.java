package com.easychat.mapper;

import org.apache.ibatis.annotations.Param;
/**
 * @Description:会话信息Mapper
 * @author:t1
 * @date:2025/11/21
 */
public interface ChatSessionMapper<T, P> extends BaseMapper {
	/**
	 * 根据SessionId查询
	 */
	T selectBySessionId(@Param("sessionId") String sessionId);

	/**
	 * 根据SessionId更新
	 */
	Integer updateBySessionId(@Param("bean") T t, @Param("sessionId") String sessionId);

	/**
	 * 根据SessionId删除
	 */
	Integer deleteBySessionId(@Param("sessionId") String sessionId);


}