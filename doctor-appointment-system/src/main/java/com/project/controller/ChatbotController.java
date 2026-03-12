package com.project.controller;

import com.project.entity.ChatLog;
import com.project.security.UserDetailsImpl;
import com.project.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/message")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();

        ChatLog responseLog = chatbotService.processMessage(userDetails.getId(), message);
        return ResponseEntity.ok(responseLog);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<?> getHistory() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        List<ChatLog> history = chatbotService.getChatHistory(userDetails.getId());
        return ResponseEntity.ok(history);
    }
}
