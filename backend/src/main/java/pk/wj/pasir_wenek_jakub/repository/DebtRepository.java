package pk.wj.pasir_wenek_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pk.wj.pasir_wenek_jakub.model.Debt;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByGroupId(Long groupId);

    // Ta metoda jest napisana świetnie i automatycznie wygeneruje poprawne zapytanie SQL,
    // o ile w encji Debt masz pole o nazwie 'createdAt'.
    List<Debt> findByGroupIdAndCreatedAtBetween(Long groupId, LocalDateTime start, LocalDateTime end);

    @Transactional
    void deleteByGroupId(Long groupId);
}