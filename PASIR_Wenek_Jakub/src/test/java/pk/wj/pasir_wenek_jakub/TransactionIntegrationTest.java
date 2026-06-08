package pk.wj.pasir_wenek_jakub; // <-- ZMIEŃ NA TWÓJ PAKIET

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- 0 i 1. SETUP & AUTH ---

    @Test
    void shouldRegisterUser() throws Exception {
        String userJson = "{\"username\": \"jan\", \"password\": \"string\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldLoginUser() throws Exception {
        String loginJson = "{\"username\": \"jan\", \"password\": \"string\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    // --- 2. POST - DODAWANIE ---

    @Test
    @WithMockUser(username = "jan")
    void shouldAddIncomeTransaction() throws Exception {
        String json = "{\"amount\": 5000.0, \"type\": \"INCOME\", \"tags\": \"salary\", \"notes\": \"Wypłata\"}";
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldFailAddingWithoutLogin() throws Exception {
        String json = "{\"amount\": 5000.0, \"type\": \"EXPENSE\", \"tags\": \"test\"}";
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized()); // 401
    }

    // --- 3. GET - WYŚWIETLANIE ---

    @Test
    @WithMockUser(username = "jan")
    void shouldGetAllTransactionsForJan() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "jan")
    void shouldNotSeeMarcinsTransaction() throws Exception {
        // Zakładamy, że ID 99 należy do Marcina
        mockMvc.perform(get("/api/transactions/2"))
                .andExpect(status().isForbidden()); // 403
    }

    // --- 4. PUT - MODYFIKACJA ---

    @Test
    @WithMockUser(username = "jan")
    void shouldUpdateOwnTransaction() throws Exception {
        String json = "{\"amount\": 6000.0, \"type\": \"INCOME\", \"tags\": \"bonus\"}";
        mockMvc.perform(put("/api/transactions/1") // ID 1 to transakcja Jana
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    // --- 5. DELETE - USUWANIE ---

    @Test
    @WithMockUser(username = "jan")
    void shouldDeleteOwnTransaction() throws Exception {
        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isNoContent()); // lub .isOk() zależnie od API
    }
}