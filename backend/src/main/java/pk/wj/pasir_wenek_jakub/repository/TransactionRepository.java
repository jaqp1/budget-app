package pk.wj.pasir_wenek_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.wj.pasir_wenek_jakub.model.Transaction;
import pk.wj.pasir_wenek_jakub.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 1. Rozwiązuje błąd: method findAllByUser(User)
    List<Transaction> findAllByUser(User user);

    // 2. Rozwiązuje błąd: method findAllByUserAndTimestampGreaterThanEqual(User, LocalDateTime)
    // Nazwa metody idealnie pasuje do Twojego pola 'timestamp' z encji Transaction!
    List<Transaction> findAllByUserAndTimestampGreaterThanEqual(User user, LocalDateTime timestamp);

    // 3. Rozwiązuje błąd: method findByUser(User)
    List<Transaction> findByUser(User user);
}