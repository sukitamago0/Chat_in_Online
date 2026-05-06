package com.easychat.entity.query;

import com.easychat.enums.PageSize;
public class SimplePage{
    private Integer pageNo;
    private Integer pageSize;
    private Integer countTotal;
    private Integer start;
    private Integer end;
    private Integer pageTotal;

    public SimplePage(){
    }

    public SimplePage(Integer pageNo, Integer countTotal, Integer pageSize){
        if(null == pageNo){
            pageNo = 0;
        }
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.countTotal = countTotal;
        action();
    }

    public SimplePage(Integer start,Integer end){
        this.start = start;
        this.end = end;
    }

    public void action(){
        if(this.pageSize <= 0){
            this.pageSize = PageSize.SIZE20.getSize();
        }
        if(this.countTotal > 0){
            this.pageTotal = this.countTotal % this.pageSize == 0 ? this.countTotal / this.pageSize
                : this.countTotal / this.pageSize + 1;
        } else {
            this.pageTotal = 1;
        }

        if(pageNo <= 1){
            pageNo = 1;
        }
        if(pageNo >= pageTotal){
            pageNo = pageTotal;
        }
        this.start = (pageNo - 1) * pageSize;
        this.end = this.pageSize;
    }

    public Integer getStart(){
        return start;
    }
    public Integer getEnd(){
        return end;
    }
    public Integer getPageTotal(){
        return pageTotal;
    }
    public Integer getPageNo(){
        return pageNo;
    }
    public void setPageNo(Integer pageNo){
        this.pageNo = pageNo;
    }
    public void setPageTotal(Integer pageTotal){
        this.pageTotal = pageTotal;
    }
    public Integer getCountTotal(){
        return countTotal;
    }
    public Integer getPageSize(){
        return pageSize;
    }
    public void setStart(Integer start){
        this.start = start;
    }
    public void setEnd(Integer end){
        this.end = end;
    }
    public void setCountTotal(Integer countTotal){
        this.countTotal = countTotal;
        this.action();
    }
    public void setPageSize(Integer pageSize){
        this.pageSize = pageSize;
    }

}
