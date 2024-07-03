package com.hmall.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.FilterVO;

import java.io.IOException;

public interface SearchService{
    Page<Item> esSearch(ItemPageQuery query) throws IOException;

    FilterVO filter(ItemPageQuery query) throws IOException;
}
