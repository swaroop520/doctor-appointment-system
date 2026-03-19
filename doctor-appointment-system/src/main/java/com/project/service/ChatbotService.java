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
        if (geminiApiKey == null || geminiApiKey.equals("REPLACE_WITH_YOUR_GEMINI_API_KEY") || geminiApiKey.isEmpty()) {
            System.out.println("ChatbotService: Gemini API Key NOT found. Using Mock Fallback.");
        } else {
            System.out.println("ChatbotService: Gemini API Key detected. Length: " + geminiApiKey.length());
        }
    }

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public ChatLog processMessage(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String response;
        if (geminiApiKey != null && !geminiApiKey.equals("REPLACE_WITH_YOUR_GEMINI_API_KEY") && !geminiApiKey.isEmpty()) {
            System.out.println("Processing with Gemini AI...");
            response = generateGeminiResponse(message);
        } else {
            System.out.println("Processing with Rule-based Mock...");
            response = generateAIResponseMock(message);
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

    private String generateGeminiResponse(String userMessage) {
        try {
            String url = GEMINI_API_URL + geminiApiKey;

            // Prepare the request body for Gemini API
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            
            // System instruction to act as a medical assistant
            String systemInstructions = "You are 'Care Connect AI', a professional medical assistant for a Doctor Appointment System. " +
                    "Analyze the user's symptoms and provide: 1. Possible Cause, 2. Immediate Precautions, 3. Recommended OTC Medicines, and 4. Which specialist doctor to book. " +
                    "Use HTML formatting (<b>, <br>, •) in your response. " +
                    "IMPORTANT: If it sounds like a life-threatening emergency, start your response with a clear warning in red. " +
                    "Keep advice general and always advise seeing a professional.";
            
            part.put("text", systemInstructions + "\n\nUser Symptom: " + userMessage);
            contents.put("parts", Collections.singletonList(part));
            
            Map<String, Object> body = new HashMap<>();
            body.put("contents", Collections.singletonList(contents));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.postForEntity(url, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseEntity.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
            return generateAIResponseMock(userMessage); // Fallback on empty/bad response
        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());
            return generateAIResponseMock(userMessage); // Fallback on error
        }
    }

    private String generateAIResponseMock(String message) {
        String lowerMsg = message.toLowerCase();
        
        // 1. Injuries, Sprains, and general Pain (Hand, Leg, Joint, Back)
        if ((lowerMsg.contains("pain") || lowerMsg.contains("hurt") || lowerMsg.contains("ache") || lowerMsg.contains("sore")) && 
            (lowerMsg.contains("hand") || lowerMsg.contains("leg") || lowerMsg.contains("arm") || lowerMsg.contains("joint") || lowerMsg.contains("knee") || lowerMsg.contains("back") || lowerMsg.contains("shoulder") || lowerMsg.contains("muscle"))) {
            return "<div style='line-height:1.5;'>" +
                   "<b>🚑 Possible Cause:</b> Muscle strain, ligament sprain, or joint inflammation.<br><br>" +
                   "<b>🛡️ Immediate Precautions:</b><br>" +
                   "• Rest the affected area and avoid heavy lifting.<br>" +
                   "• Apply an ice pack for 15-20 minutes every 2-3 hours.<br>" +
                   "• Keep the area elevated if possible.<br><br>" +
                   "<b>💊 Recommended Medicines (OTC):</b><br>" +
                   "• Pain relievers like Ibuprofen or Acetaminophen can help reduce pain and swelling.<br>" +
                   "• Pain-relief gels/sprays (e.g., Volini, Moov) for topical application.<br><br>" +
                   "<b>🩺 When to see a doctor:</b> If the pain is severe, accompanied by visible deformity, unable to bear weight, or doesn't improve after a few days, please book an appointment with an Orthopedist." +
                   "</div>";
        }
        
        // 2. Stomach Issues
        else if (lowerMsg.contains("stomach") || lowerMsg.contains("belly") || lowerMsg.contains("abdomen")) {
            return "<div style='line-height:1.5;'>" +
                   "<b>🚑 Possible Cause:</b> Indigestion, gastritis, food poisoning, or trapped gas.<br><br>" +
                   "<b>🛡️ Immediate Precautions:</b><br>" +
                   "• Rest and stay hydrated with small sips of water or clear fluids (broth, ORS).<br>" +
                   "• Avoid spicy, greasy, or dairy-heavy foods.<br>" +
                   "• Use a heating pad on your abdomen for comfort.<br><br>" +
                   "<b>💊 Recommended Medicines (OTC):</b><br>" +
                   "• Antacids (e.g., Digene, Gelusil) for acidity.<br>" +
                   "• Anti-spasmodic meds (e.g., Meftal-Spas) for cramps <i>(Use cautiously)</i>.<br><br>" +
                   "<b>🩺 When to see a doctor:</b> If the pain is sharp/sudden, accompanied by persistent vomiting, high fever, or blood in stool, consult a Gastroenterologist immediately." +
                   "</div>";
        }
        
        // 3. Fever, Cold, Headline
        else if (lowerMsg.contains("fever") || lowerMsg.contains("cold") || lowerMsg.contains("headache") || lowerMsg.contains("cough")) {
            return "<div style='line-height:1.5;'>" +
                   "<b>🚑 Possible Cause:</b> Viral infection, common cold, or seasonal flu.<br><br>" +
                   "<b>🛡️ Immediate Precautions:</b><br>" +
                   "• Get plenty of rest and drink adequate fluids (water, warm soups).<br>" +
                   "• Do warm salt-water gargles for a sore throat.<br>" +
                   "• Use steam inhalation for nasal congestion.<br><br>" +
                   "<b>💊 Recommended Medicines (OTC):</b><br>" +
                   "• Paracetamol (e.g., Dolo 650, Crocin) for fever and headache.<br>" +
                   "• Antihistamines (e.g., Cetirizine) for runny nose.<br>" +
                   "• Cough syrups (based on dry/wet cough type).<br><br>" +
                   "<b>🩺 When to see a doctor:</b> If the fever exceeds 102°F (38.9°C), persists for more than 3 days, or is accompanied by severe chest pain/breathing difficulty, please book a General Physician appointment." +
                   "</div>";
        }
        
        // 4. Skin Issues, Allergies, and Hair Fall
        else if (lowerMsg.contains("hair fall") || lowerMsg.contains("hair loss") || lowerMsg.contains("dandruff")) {
            return "<div style='line-height:1.5;'>" +
                   "<b>🚑 Possible Cause:</b> Stress, nutritional deficiency, hormonal changes, or scalp infections.<br><br>" +
                   "<b>🛡️ Precautions & Care:</b><br>" +
                   "• Ensure a balanced diet rich in Protein, Iron, and Vitamins.<br>" +
                   "• Avoid harsh chemicals, tight hairstyles, and excessive heat styling.<br>" +
                   "• Manage stress through yoga or meditation.<br><br>" +
                   "<b>💊 Recommended Products (OTC):</b><br>" +
                   "• Mild sulfate-free shampoos or Ketoconazole shampoos (for dandruff).<br>" +
                   "• Biotin supplements (consult doctor first).<br><br>" +
                   "<b>🩺 When to see a doctor:</b> If hair fall is sudden, in patches, or accompanied by scalp itching/redness, please consult a Dermatologist." +
                   "</div>";
        }
        else if (lowerMsg.contains("skin") || lowerMsg.contains("rash") || lowerMsg.contains("acne") || lowerMsg.contains("itching") || lowerMsg.contains("allergy")) {
            return "<div style='line-height:1.5;'>" +
                   "<b>🚑 Possible Cause:</b> Allergic reaction, contact dermatitis, eczema, or bacterial/fungal infection.<br><br>" +
                   "<b>🛡️ Precautions & Care:</b><br>" +
                   "• Keep the skin clean and dry. Avoid scratching.<br>" +
                   "• Wear loose, breathable cotton clothing.<br>" +
                   "• Avoid suspected allergens (new cosmetics, certain foods).<br><br>" +
                   "<b>💊 Recommended Medicines (OTC):</b><br>" +
                   "• Calamine lotion or Aloe Vera gel for soothing.<br>" +
                   "• Oral Antihistamines (e.g., Cetirizine) for severe itching.<br><br>" +
                   "<b>🩺 When to see a doctor:</b> If the rash spreads rapidly, is painful, oozing pus, or accompanied by fever/breathing issues, see a Dermatologist immediately." +
                   "</div>";
        }
        
        // 5. Emergency Keywords
        else if (lowerMsg.contains("emergency") || lowerMsg.contains("heart") || lowerMsg.contains("accident") || lowerMsg.contains("bleed")) {
            return "<div style='color:var(--danger); font-weight:bold; font-size:1.1rem; line-height:1.5;'>" +
                   "⚠️ THIS SOUNDS LIKE A MEDICAL EMERGENCY.<br><br>" +
                   "Please immediately go to the nearest hospital.<br>" +
                   "You can use the 'Emergency Slot' feature on your dashboard for immediate booking, but do not wait for online consultation." +
                   "</div>";
        }
        
        // 5. Default Fallback
        else {
            return "<div style='line-height:1.5;'>" +
                   "<b>🤖 AI Assistant:</b><br><br>" +
                   "I am an AI assistant designed to provide general medical advice and recommend doctors based on your symptoms.<br><br>" +
                   "To help me assist you better, please describe your main symptom clearly.<br>" +
                   "<i>Example: 'I have a severe headache and fever' or 'My lower back hurts.'</i>" +
                   "</div>";
        }
    }
}
