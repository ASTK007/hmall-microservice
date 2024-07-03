package com.hmall.search.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.FilterVO;
import com.hmall.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.23.12.148:9200")
    ));

    @Override
    public Page<Item> esSearch(ItemPageQuery query) throws IOException {

        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        // 1.创建Request
        SearchRequest request = new SearchRequest("items");
        // 2.组织请求参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.should(QueryBuilders.constantScoreQuery(termQuery("isAD","true")).boost(2.0f));
        bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        if (query.getBrand() != null && !query.getBrand().isEmpty()) {
            bool.filter(termQuery("brand", query.getBrand()));
        }
        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            bool.filter(termQuery("category", query.getCategory()));
        }
        if (query.getMaxPrice() != null && query.getMinPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()).gte(query.getMinPrice()));
        }
        request.source().query(bool);
        // 2.2.排序参数
        request.source().sort("price", SortOrder.ASC);
        // 2.3.分页参数
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        ArrayList<Item> items = handleResponse(response);
        Page<Item> page = new Page<>(query.getPageNo(), query.getPageSize());
        page.setRecords(items);

        return page;
    }

    @Override
    public FilterVO filter(ItemPageQuery query) throws IOException {
        SearchRequest request = new SearchRequest("items");
        // 2.准备请求参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        if(query.getBrand() != null && !query.getBrand().isEmpty()){
            bool.filter(termQuery("brand", query.getBrand()));
        }
        if(query.getCategory() != null && !query.getCategory().isEmpty()){
            bool.filter(termQuery("category", query.getCategory()));
        }
        request.source().query(bool).size(0);
        // 3.聚合参数
        request.source().aggregation(
                AggregationBuilders.terms("brand_agg").field("brand").size(10)
        );
        request.source().aggregation(
                AggregationBuilders.terms("category_agg").field("category").size(10)
        );
        // 4.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 5.解析聚合结果
        Aggregations aggregations = response.getAggregations();
        // 5.1.获取品牌聚合
        Terms brandTerms = aggregations.get("brand_agg");
        // 5.2.获取分类聚合
        Terms categoryTerms = aggregations.get("category_agg");
        // 5.3.获取聚合中的桶
        List<? extends Terms.Bucket> brandBuckets = brandTerms.getBuckets();
        List<? extends Terms.Bucket> categoryBuckets = categoryTerms.getBuckets();
        // 5.4.遍历桶内数据
        ArrayList<String> categories = new ArrayList<>();
        ArrayList<String> brands = new ArrayList<>();
        for (Terms.Bucket bucket : brandBuckets) {
            // 5.5.获取桶内key
            brands.add(bucket.getKeyAsString());
        }
        for (Terms.Bucket bucket : categoryBuckets) {
            // 5.6.获取桶内key
            categories.add(bucket.getKeyAsString());
        }
        // 5.9.创建并返回结果
        FilterVO result = new FilterVO();
        result.setBrand(brands);
        result.setCategory(categories);
        return result;
    }


    private ArrayList<Item> handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据");
        // 2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        ArrayList<Item> items = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化并打印
            Item item = JSONUtil.toBean(source, Item.class);
            items.add(item);
        }
        return items;
    }
}
