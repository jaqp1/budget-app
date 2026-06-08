package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import pk.wj.pasir_wenek_jakub.dto.BalanceDTO;
import pk.wj.pasir_wenek_jakub.dto.TransactionDTO;
import pk.wj.pasir_wenek_jakub.model.Transaction;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.service.TransactionService;
import pk.wj.pasir_wenek_jakub.dto.BalanceDTO;
import pk.wj.pasir_wenek_jakub.model.User;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @MutationMapping
    public Transaction addTransaction(
            @Valid @Argument TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO);
    }

    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
            @Valid @Argument TransactionDTO transactionDTO) {
        return transactionService.updateTransaction(id, transactionDTO);
    }

    @MutationMapping
    public Boolean deleteTransaction(@Argument Long id) {
        return transactionService.deleteTransaction(id);
    }

    @QueryMapping
    public BalanceDTO userBalance(@Argument Integer days) {
        User user = transactionService.getCurrentUser();
        // Przekazujemy argument 'days' do serwisu
        return transactionService.getUserBalance(user, days);
    }
}