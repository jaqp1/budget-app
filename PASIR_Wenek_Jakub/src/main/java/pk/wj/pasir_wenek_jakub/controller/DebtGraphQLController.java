package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.wj.pasir_wenek_jakub.dto.DebtDTO;
import pk.wj.pasir_wenek_jakub.dto.GroupMemberBalanceDTO;
import pk.wj.pasir_wenek_jakub.model.Debt;
import pk.wj.pasir_wenek_jakub.service.DebtService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DebtGraphQLController {
    private final DebtService debtService;

    public DebtGraphQLController(DebtService debtService) {
        this.debtService = debtService;
    }

    /**
     * Mapowanie zapytania 'groupDebts'.
     * Zwraca listę wszystkich długów wewnątrz wskazanej grupy.
     */
    @QueryMapping
    public List<Debt> groupDebts(@Argument Long groupId) {
        return debtService.getGroupDebts(groupId);
    }

    /**
     * Mapowanie mutacji 'createDebt'.
     * Pozwala na ręczne zdefiniowanie długu między dwiema osobami w grupie.
     */
    @MutationMapping
    public Debt createDebt(@Valid @Argument DebtDTO debtDTO) {
        return debtService.createDebt(debtDTO);
    }

    /**
     * Mapowanie mutacji 'deleteDebt'.
     * Usuwa dług o podanym identyfikatorze.
     */
    @MutationMapping
    public Boolean deleteDebt(@Argument Long debtId) {
        debtService.deleteDebt(debtId);
        return true;
    }
    @MutationMapping
    public Debt markDebtAsPaid(@Argument Long debtId){
        return debtService.markDebtAsPaid(debtId);
    }
    @MutationMapping
    public Debt confirmDebtPayment(@Argument Long debtId){
        return debtService.confirmDebtPayment(debtId);
    }

    @QueryMapping
    public List<GroupMemberBalanceDTO> groupBalance(
            @Argument Long groupId,
            @Argument LocalDate startDate,
            @Argument LocalDate endDate) {

        // 1. Pobieramy mapę z serwisu (Klucz: ID użytkownika, Wartość: kwota)
        Map<Long, Double> balanceMap = debtService.calculateGroupBalance(groupId, startDate, endDate);

        // 2. Konwertujemy mapę na listę obiektów DTO akceptowaną przez schemat GraphQL
        return balanceMap.entrySet().stream()
                .map(entry -> new GroupMemberBalanceDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    }