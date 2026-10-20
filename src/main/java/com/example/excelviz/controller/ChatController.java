package com.example.excelviz.controller;

import com.example.excelviz.model.ChatRequest;
import com.example.excelviz.model.ChatResponse;
import com.example.excelviz.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatbotService chatbotService;

    @Autowired
    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        log.debug("Chat inquiry received: {}", request.getMessage());
        ChatResponse response = chatbotService.processQuery(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions() {
        return ResponseEntity.ok(chatbotService.getDefaultSuggestions());
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clear() {
        return ResponseEntity.ok(Map.of("message", "Conversation reset"));
    }
}
