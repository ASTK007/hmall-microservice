package com.hmall.search.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.FilterVO;
import com.hmall.search.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController
{
    private final SearchService searchService;

    @ApiOperation("商品搜索")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        Page<Item> result = searchService.esSearch(query);
        return PageDTO.of(result, ItemDTO.class);
    }

    @ApiOperation("过滤条件聚合")
    @PostMapping("/filters")
    public FilterVO filter(ItemPageQuery query) throws IOException {
        return searchService.filter(query);
    }
}
