package pk.wj.pasir_wenek_jakub.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.wj.pasir_wenek_jakub.dto.GroupNotificationDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Mapa przechowująca aktywne sesje: Klucz = Email użytkownika, Wartość = Sesja WS
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("userEmail");
        if (email != null) {
            sessions.put(email, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String email = (String) session.getAttributes().get("userEmail");
        if (email != null) {
            sessions.remove(email);
        }
    }

    // Metoda do wysyłania spersonalizowanego powiadomienia
    public void sendNotification(String userEmail, GroupNotificationDTO notification) {
        WebSocketSession session = sessions.get(userEmail);
        if (session != null && session.isOpen()) {
            try {
                String jsonPayload = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(jsonPayload));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendNotificationToUser(@Email(message = "Podaj poprawny adres e-mail") @NotBlank(message = "Adres e-mail jest wymagany") String email, GroupNotificationDTO notification) {
    }
}