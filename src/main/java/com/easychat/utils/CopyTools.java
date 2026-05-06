package com.easychat.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CopyTools {
    // 复制List
    public static <T, S> List<T> copyList(List<S> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>();
        for (S s : sourceList) {
            T t = null;
            try{
                t = targetClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    // 复制对象
    public static <T, S> T copy(S s, Class<T> targetClass) {
        T t = null;
        try{
            t = targetClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }
}
