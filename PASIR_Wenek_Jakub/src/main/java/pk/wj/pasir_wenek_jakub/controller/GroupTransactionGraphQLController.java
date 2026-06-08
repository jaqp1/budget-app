package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.wj.pasir_wenek_jakub.dto.GroupTransactionDTO;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.service.CurrentUserService;
import pk.wj.pasir_wenek_jakub.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {
    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(
            GroupTransactionService groupTransactionService,
            CurrentUserService currentUserService) {
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    /**
     * Mapowanie mutacji 'addGroupTransaction'.
     * Pobiera aktualnie zalogowanego użytkownika i zleca serwisowi
     * rozliczenie transakcji między członkami grupy.
     */
    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO) {
        User user = currentUserService.getCurrentUser();
        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}