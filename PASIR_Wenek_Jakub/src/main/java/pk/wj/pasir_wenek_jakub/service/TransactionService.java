package pk.wj.pasir_wenek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pk.wj.pasir_wenek_jakub.dto.BalanceDTO;
import pk.wj.pasir_wenek_jakub.dto.TransactionDTO;
import pk.wj.pasir_wenek_jakub.model.Transaction;
import pk.wj.pasir_wenek_jakub.model.TransactionType;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.TransactionRepository;
import pk.wj.pasir_wenek_jakub.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // --- READ (Pobieranie) ---

    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        // Zwracamy tylko transakcje należące do zalogowanego usera
        return transactionRepository.findAllByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        // SPRAWDZENIE WŁAŚCICIELA
        checkTransactionOwnership(transaction);

        return transaction;
    }

    // --- CREATE (Tworzenie - POST) ---

    @Transactional
    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        mapDtoToEntity(transactionDTO, transaction);
        transaction.setUser(getCurrentUser());
        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    // --- UPDATE (Aktualizacja - PUT) ---

    @Transactional
    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        // SPRAWDZENIE WŁAŚCICIELA (Listing 4.9)
        checkTransactionOwnership(transaction);

        mapDtoToEntity(transactionDTO, transaction);
        return transactionRepository.save(transaction);
    }

    // --- DELETE (Usuwanie - DELETE) ---

    @Transactional
    public Boolean deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie można usunąć. Nie znaleziono transakcji o ID " + id));

        checkTransactionOwnership(transaction);

        transactionRepository.delete(transaction);
        return null;
    }

    // --- METODY POMOCNICZE ---

    // Nowa metoda pomocnicza, żeby nie powtarzać IF-a w każdej metodzie
    private void checkTransactionOwnership(Transaction transaction) {
        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }
    }

    private void mapDtoToEntity(TransactionDTO dto, Transaction entity) {
        entity.setAmount(dto.getAmount());
        entity.setType(TransactionType.valueOf(dto.getType()));
        entity.setTags(dto.getTags());
        entity.setNotes(dto.getNotes());
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Użytkownik nie jest uwierzytelniony");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zalogowanego użytkownika: " + email));
    }
    // Ścieżka: src/main/java/pk/ni/pasir_nazwisko_imie/service/TransactionService.java

    public BalanceDTO getUserBalance(User user, Integer days) {
        List<Transaction> userTransactions;

        if (days != null && days > 0) {
            // Obliczamy datę startową (np. teraz minus 7 dni)
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            userTransactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, startDate);
        } else {
            // Jeśli nie podano dni, pobieramy całą historię
            userTransactions = transactionRepository.findByUser(user);
        }

        double income = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return new BalanceDTO(income, expense, income - expense);
    }
}