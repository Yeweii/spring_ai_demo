package com.itzixi.service;

import com.itzixi.bean.SearchResult;

import java.util.List;

public interface SesrXngService {


    /**
     * @Description: 调用本地搜索引擎searxng进行搜索
     * @Author
     * @param query
     * @return List<SearchResult>
     */
     List<SearchResult> search(String query);
}
