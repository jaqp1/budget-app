package pk.wj.pasir_wenek_jakub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String tags;
    private String notes;
    private LocalDateTime timestamp;

    // Wszystkie gettery, settery i konstruktor bezargumentowy zostały usunięte.
    // Lombok wygeneruje je "w locie" podczas kompilacji.

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    // Konstruktor z parametrami
    public Transaction(Double amount, TransactionType type, String tags, String notes, User user) {
        this.amount = amount;
        this.type = type;
        this.tags = tags;
        this.notes = notes;
        this.user = user;
        this.timestamp = LocalDateTime.now();
    }
}