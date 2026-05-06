package com.easychat.mapper;

import org.apache.ibatis.annotations.Param;
/**
 * @Description:聊天消息表Mapper
 * @author:t1
 * @date:2025/11/21
 */
public interface ChatMessageMapper<T, P> extends BaseMapper {
	/**
	 * 根据MessageId查询
	 */
	T selectByMessageId(@Param("messageId") Long messageId);

	/**
	 * 根据MessageId更新
	 */
	Integer updateByMessageId(@Param("bean") T t, @Param("messageId") Long messageId);

	/**
	 * 根据MessageId删除
	 */
	Integer deleteByMessageId(@Param("messageId") Long messageId);


}