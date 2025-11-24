package com.itzixi.controller;

import com.itzixi.bean.ChatEntity;
import com.itzixi.service.ChatService;
import com.itzixi.service.DocumentService;
import com.itzixi.utils.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @Resource
    private ChatService chatService;

    @PostMapping("uploadRagDoc")
    public Result uploadRagDoc(@RequestParam ("file") MultipartFile file) {
        List<Document> documents = documentService.loadText(file.getResource(), file.getName());
        return Result.ok(documents);
    }

    @GetMapping("doSearch")
    public Result doSearch(@RequestParam String question) {
        List<Document> documents = documentService.doSearch(question);
        return Result.ok(documents);
    }

    @PostMapping("/search")
    public void search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {

        List<Document> list = documentService.doSearch(chatEntity.getMessage());
        response.setCharacterEncoding("UTF-8");
        chatService.doChatRagSearch(chatEntity, list);
    }

}
