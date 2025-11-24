package com.itzixi.service;


import com.itzixi.bean.ChatEntity;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    /**
     * 聊天测试方法
     * @param msg 聊天消息
     * @return 聊天回复
     */
    String chatTest(String msg);

    /**
     *
     * @param prompt
     * @return
     */
    Flux<String> streamStr(String prompt);

    void doChat(ChatEntity chatEntity);

    void doChatRagSearch(ChatEntity chatEntity, List<Document> list);
}
