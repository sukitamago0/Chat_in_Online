package com.easychat.mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BaseMapper<T,P> {

    /**
     * Insert
     */
    Integer insert(@Param("bean") T t);

    /**
    * Insert or Update.
    */
    Integer insertOrUpdate(@Param("bean") T t);


    /**
     * Insert multiple records into the database in a batch operation.
     */
    Integer insertBatch(@Param("list") List<T> list);

     /**
     * Insert or Update Batch.
     */
    Integer insertOrUpdateBatch(@Param("list") List<T> list);

    /**
     * 根据参数查询集合
     */
    List<T> selectList(@Param("query") P p);

    /**
     * SelectCount(根据集合查询总数)
     */
    Integer selectCount(@Param("query") P p);
}
