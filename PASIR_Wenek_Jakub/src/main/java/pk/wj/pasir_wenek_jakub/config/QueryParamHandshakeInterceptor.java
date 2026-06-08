package pk.wj.pasir_wenek_jakub.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import pk.wj.pasir_wenek_jakub.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class QueryParamHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public QueryParamHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            String token = req.getParameter("token");

            if (token != null && jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token); // Dostosuj metodę do swojej implementacji JwtUtil
                attributes.put("userEmail", email);
                return true;
            }
        }
        return false; // Odrzucenie połączenia, jeśli brak tokenu lub jest nieprawidłowy
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}