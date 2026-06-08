package pk.wj.pasir_wenek_jakub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public WebSecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/graphql").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Zezwól na port, na którym działa Twój frontend (Vite)
        configuration.setAllowedOrigins(List.of("http://localhost:5174"));
        // Zezwól na wszystkie popularne metody HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Zezwól na wszystkie nagłówki (ważne przy przesyłaniu Tokenów JWT)
        configuration.setAllowedHeaders(List.of("*"));
        // Pozwól na przesyłanie ciasteczek/autoryzacji (jeśli będzie potrzebne)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}