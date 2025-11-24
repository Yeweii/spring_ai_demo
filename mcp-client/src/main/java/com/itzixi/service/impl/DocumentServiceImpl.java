package com.itzixi.service.impl;

import com.itzixi.service.DocumentService;
import com.itzixi.utils.CustomTextSplitter;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final RedisVectorStore redisVectorStore;

    /**
     * @param resource
     * @param fileName
     * @Description: 加载文档并且读取数据进行保存到知识库
     * @Author 风间影月
     */
    @Override
    public List<Document> loadText(Resource resource, String fileName) {

        // 读取文档
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("fileName", fileName);
        List<Document> documents = textReader.get();

        CustomTextSplitter customTextSplitter = new CustomTextSplitter();

        List<Document> list = customTextSplitter.apply(documents);
//        log.info("list: {}", list);

        System.out.println("list = " + list);

        redisVectorStore.doAdd(list);

        return documents;
    }

    /**
     * @param question
     * @return List<Document>
     * @Description: 根据提问从知识库中查询相应的知识/资料（相似）
     * @Author 风间影月
     */
    @Override
    public List<Document> doSearch(String question) {
        return redisVectorStore.similaritySearch(question);
    }


}
