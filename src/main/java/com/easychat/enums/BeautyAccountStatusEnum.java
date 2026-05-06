package com.easychat.enums;

public enum BeautyAccountStatusEnum {

    NO_USE(0, "未使用"),
    USED(1, "已使用");

    private Integer status;
    private String desc;

     BeautyAccountStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
