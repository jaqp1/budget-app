package pk.wj.pasir_wenek_jakub.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class BalanceFilterDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}