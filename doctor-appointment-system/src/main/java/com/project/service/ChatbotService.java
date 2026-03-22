package com.project.service;

import com.project.entity.ChatLog;
import com.project.entity.User;
import com.project.repository.ChatLogRepository;
import com.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatbotService {

    @Autowired
    private ChatLogRepository chatLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${google.gemini.api.key}")
    private String geminiApiKey;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.contains("AIzaSy")) {
            System.err.println("CRITICAL: Gemini API Key is missing, empty, or leaked!");
            System.err.println("Please set the GEMINI_API_KEY environment variable.");
        } else {
            String masked = geminiApiKey.substring(0, Math.min(4, geminiApiKey.length())) + "....";
            System.out.println("ChatbotService: Gemini API Key detected! Starts with: " + masked);
        }
    }

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public ChatLog processMessage(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String response;
        String geminiError = null;

        if (geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.contains("REPLACE")) {
            System.out.println("Processing with Gemini AI...");
            try {
                response = generateGeminiResponse(message);
                if (response != null && response.startsWith("ERROR_FROM_GEMINI: ")) {
                    geminiError = response.substring(19);
                    response = generateAIResponseMock(message, geminiError);
                }
            } catch (Exception e) {
                geminiError = e.getMessage();
                response = generateAIResponseMock(message, geminiError);
            }
        } else {
            System.out.println("Processing with Rule-based Mock...");
            response = generateAIResponseMock(message, "Key missing or placeholder");
        }

        ChatLog chatLog = new ChatLog();
        chatLog.setUser(user);
        chatLog.setMessage(message);
        chatLog.setResponse(response);
        chatLog.setTimestamp(LocalDateTime.now());

        return chatLogRepository.save(chatLog);
    }

    public List<ChatLog> getChatHistory(Long userId) {
        return chatLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @SuppressWarnings("unchecked")
    private String generateGeminiResponse(String userMessage) {
        try {
            String url = GEMINI_API_URL + geminiApiKey;

            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            
            String systemInstructions = "You are 'Care Connect AI', a professional medical assistant. Provide a highly simplified response in AT MOST 5 lines. Use HTML (<b>, <br>, •). Be concise and prioritize the most important advice.";
            
            part.put("text", systemInstructions + "\n\nUser Symptom: " + userMessage);
            contents.put("parts", Collections.singletonList(part));
            
            Map<String, Object> body = new HashMap<>();
            body.put("contents", Collections.singletonList(contents));
            
            // Allow more medical-related content by lowering safety thresholds
            List<Map<String, String>> safetySettings = new ArrayList<>();
            String[] categories = {
                "HARM_CATEGORY_HARASSMENT", 
                "HARM_CATEGORY_HATE_SPEECH", 
                "HARM_CATEGORY_SEXUALLY_EXPLICIT", 
                "HARM_CATEGORY_DANGEROUS_CONTENT"
            };
            for (String category : categories) {
                Map<String, String> setting = new HashMap<>();
                setting.put("category", category);
                setting.put("threshold", "BLOCK_NONE"); // or BLOCK_ONLY_HIGH
                safetySettings.add(setting);
            }
            body.put("safetySettings", safetySettings);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            int maxRetries = 3;
            int retryCount = 0;
            long waitTime = 1000; // Start with 1 second

            while (retryCount < maxRetries) {
                try {
                    // Using raw Map class to avoid generic type mismatch during build
                    ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);

                    if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                        Map<String, Object> resBody = (Map<String, Object>) responseEntity.getBody();
                        if (resBody != null) {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) resBody.get("candidates");
                            if (candidates != null && !candidates.isEmpty()) {
                                Map<String, Object> candidate = candidates.get(0);
                                if (candidate != null) {
                                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                                    if (content != null) {
                                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                                        if (parts != null && !parts.isEmpty()) {
                                            return (String) parts.get(0).get("text");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break; // Exit loop if OK but structure invalid
                } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        System.err.println("Gemini Daily Quota Exceeded. Switching to Rule-based Fallback.");
                        return getStaticFallbackMessage(userMessage);
                    }
                    System.out.println("Gemini Rate Limit (429) hit. Retrying in " + waitTime + "ms... (Attempt " + retryCount + ")");
                    Thread.sleep(waitTime);
                    waitTime *= 2; // Exponential increase
                } catch (Exception e) {
                    System.err.println("Gemini API Error: " + e.getMessage());
                    return getStaticFallbackMessage(userMessage); // Fallback for any API failure
                }
            }
            return getStaticFallbackMessage(userMessage);
        } catch (Exception e) {
            return getStaticFallbackMessage(userMessage);
        }
    }

    private String getStaticFallbackMessage(String prompt) {
        String msg = prompt.toLowerCase();
        StringBuilder sb = new StringBuilder("<b>[SAFETY MODE: Knowledge Base Advice]</b><br>");
        boolean found = false;

        // EMERGENCY - Check first for high priority
        if (msg.contains("heart") || msg.contains("chest") || msg.contains("breath") || msg.contains("chok") || msg.contains("unconscious")) {
            sb.append("• <b>🚨 CRITICAL EMERGENCY:</b> If you are experiencing chest pain, difficulty breathing, or heart-related symptoms, <b>CALL EMERGENCY SERVICES (e.g., 911 or 108) IMMEDIATELY.</b> Do not wait for a response.<br>");
            found = true;
        }

        if (msg.contains("fever") || msg.contains("temperature") || msg.contains("feverish") || msg.contains("hot body")) {
            sb.append("• <b>Fever:</b> Use Paracetamol (Acetaminophen) for relief. Drink plenty of water and rest. Visit a doctor if fever persists for >3 days.<br>");
            found = true;
        }
        if (msg.contains("cold") || msg.contains("cough") || msg.contains("flu") || msg.contains("nose") || msg.contains("throat") || msg.contains("sneez")) {
            sb.append("• <b>Cold/Cough:</b> Stay hydrated with warm liquids. Use steam inhalation or saline drops for congestion.<br>");
            found = true;
        }
        if (msg.contains("headache") || msg.contains("head ache") || msg.contains("migraine") || msg.contains("dizzy") || msg.contains("giddiness")) {
            sb.append("• <b>Head/Dizziness:</b> Rest in a quiet, dark room. Ensure you are hydrated.<br>");
            found = true;
        }
        if (msg.contains("skin") || msg.contains("allergy") || msg.contains("rash") || msg.contains("itch") || msg.contains("pimple") || msg.contains("acne")) {
            sb.append("• <b>Skin/Allergy:</b> Clean the area gently. For minor rashes, an over-the-counter antihistamine may help.<br>");
            found = true;
        }
        if ((msg.contains("stomach") || msg.contains("belly") || msg.contains("gastric") || msg.contains("digestion") || msg.contains("vomit")) && !msg.contains("heart")) {
            sb.append("• <b>Stomach/Digestion:</b> Stick to a bland diet (BRAT). Stay hydrated. If pain is severe or localized, consult a doctor.<br>");
            found = true;
        }
        if ((msg.contains("injury") || msg.contains("cut") || msg.contains("wound") || msg.contains("bone") || msg.contains("fracture") || msg.contains("swelling")) || (msg.contains("pain") && !found)) {
            sb.append("• <b>General Pain/Injury:</b> Use the RICE method (Rest, Ice, Compression, Elevation). Clean minor cuts. If pain is severe, visit the clinic.<br>");
            found = true;
        }
        if (msg.contains("tablet") || msg.contains("medicine") || msg.contains("syrup") || msg.contains("pill")) {
            sb.append("• <b>Medication Note:</b> Only common over-the-counter (OTC) meds are suggested for minor symptoms. Consult a doctor for dosages.<br>");
            found = true;
        }

        if (!found) {
            sb.append("👋 Hello! I'm currently in Safety Mode due to AI quota limits. For medical symptoms, please describe them (e.g., 'I have a fever'). For appointments, use the Dashboard features.");
        } else {
            sb.append("<br><i>Note: Providing verified care guidelines while AI quota is resetting.</i>");
        }

        return sb.toString();
    }

    private String generateAIResponseMock(String message, String geminiError) {
        String lowerMsg = message.toLowerCase();
        String response = "";
        
        if ((lowerMsg.contains("pain") || lowerMsg.contains("hurt") || lowerMsg.contains("ache")) && 
            (lowerMsg.contains("hand") || lowerMsg.contains("leg") || lowerMsg.contains("back") || lowerMsg.contains("muscle"))) {
            response = "<div style='line-height:1.5;'><b>🚑 Possible Cause:</b> Muscle issues.<br><b>🛡️ Precautions:</b> Rest.</div>";
        } else if (lowerMsg.contains("stomach") || lowerMsg.contains("belly")) {
            response = "<div style='line-height:1.5;'><b>🚑 Possible Cause:</b> Indigestion.<br><b>🛡️ Precautions:</b> Hydrate.</div>";
        } else if (lowerMsg.contains("fever") || lowerMsg.contains("cold") || lowerMsg.contains("headache")) {
            response = "<div style='line-height:1.5;'><b>🚑 Possible Cause:</b> Viral infection.<br><b>🛡️ Precautions:</b> Rest.</div>";
        } else {
            response = "<div style='line-height:1.5;'><b>🤖 AI Assistant:</b> Please describe symptoms more clearly.</div>";
        }

        String versionInfo = "\n\n<small style='color: gray;'>Backend Version: 1.1.0-STABLE-URL</small>";
        String debugInfo = "\n\n---\n<b>DEBUG INFO:</b> " + 
            (geminiApiKey == null ? "Key NULL" : "Key OK (" + geminiApiKey.length() + " chars)");
        
        if (geminiError != null) {
            debugInfo += "\n\n<b>GEMINI ERROR:</b> <span style='color:red;'>" + geminiError + "</span>";
        }
        
        return response + versionInfo + debugInfo;
    }

    private String generateAIResponseMock(String message) {
        return generateAIResponseMock(message, null);
    }
}
