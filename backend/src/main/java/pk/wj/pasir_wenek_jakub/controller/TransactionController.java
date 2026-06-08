package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pk.wj.pasir_wenek_jakub.dto.TransactionDTO;
import pk.wj.pasir_wenek_jakub.model.Transaction;
import pk.wj.pasir_wenek_jakub.service.TransactionService;


import java.util.List;

@RestController
@RequestMapping("/api/transactions") // Przykładowy mapping bazowy
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // 1. Downloading all transactions (GET)
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // 2. Retrieving a single transaction by ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    // 3. Updating an existing transaction (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO transactionDTO) {
        Transaction updatedTransaction = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(updatedTransaction);
    }

    // 4. Creating a new transaction (POST) - Dodane samodzielnie
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        Transaction newTransaction = transactionService.createTransaction(transactionDTO);
        // Zwracamy status 201 Created dla operacji POST
        return new ResponseEntity<>(newTransaction, HttpStatus.CREATED);
    }

    // 5. Deleting a transaction (DELETE) - Dodane samodzielnie
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        // Zwracamy status 204 No Content, bo transakcja została usunięta
        return ResponseEntity.noContent().build();
    }
}