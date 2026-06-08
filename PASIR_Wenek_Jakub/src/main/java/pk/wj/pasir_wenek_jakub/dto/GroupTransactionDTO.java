package pk.wj.pasir_wenek_jakub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class GroupTransactionDTO {

    @NotNull(message = "Id grupy nie może być puste")
    private Long groupId; // Group ID

    @NotNull(message = "Kwota nie może być pusta")
    @Positive(message = "Kwota musi być większa od zera")
    private Double amount; // Amount = + earnings - expense

    @NotBlank(message = "Typ transakcji nie może być pusty")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Typ transakcji musi mieć wartość INCOME albo EXPENSE")
    private String type; // INCOME or EXPENSE

    @NotBlank(message = "Tytuł nie może być pusty")
    @Size(max = 100, message = "Tytuł nie może przekraczać 100 znaków")
    private String title; // Transaction title

    private List<Long> selectedUserIds;

}