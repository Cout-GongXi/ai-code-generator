package com.zzy.aicodegenerator.common;

import lombok.Data;

@Data
public class PageRequest {
    /**
     * 当前页码，默认为1
     */
    private int pageNum = 1;
    /**
     * 页面大小，默认为10
     */
    private int pageSize = 10;
    /**
     * 排序字段，默认为空字符串
     */
    private String sortField;
    /**
     * 排序顺序，默认为降序
     */
    private String sortOrder = "descend";
}
