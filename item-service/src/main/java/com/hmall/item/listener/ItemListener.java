package com.hmall.item.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.controller.ItemController;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.aspectj.lang.annotation.Before;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemListener {

    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.23.12.148:9200")
    ));

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.add.queue", durable = "true"),
            exchange = @Exchange(name = "item.topic"),
            key = "item.add"
    ))
    public void listenItemAdd(Item item) throws IOException {
        log.info("接收到新增商品消息：{}", item);
        // 2.转换为文档类型
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        // 3.将ItemDTO转json
        String doc = JSONUtil.toJsonStr(itemDoc);

        // 1.准备Request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 2.准备Json文档
        request.source(doc, XContentType.JSON);
        // 3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.update.queue", durable = "true"),
            exchange = @Exchange(name = "item.topic"),
            key = "item.update"
    ))
    public void listenItemUpdate(Item item) throws IOException {
        log.info("接收到更新商品消息：{}", item);
        // 1.准备Request
        UpdateRequest request = new UpdateRequest("items", item.getId().toString());
        // 2.准备请求参数
        request.doc(
                "status", item.getStatus()
        );
        // 3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "item.topic"),
            key = "item.delete"
    ))
    public void listenItemDelete(Long id) throws IOException {
        log.info("接收到删除商品消息：{}", id);
        // 1.准备Request，两个参数，第一个是索引库名，第二个是文档id
        DeleteRequest request = new DeleteRequest("items", id.toString());
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }
}