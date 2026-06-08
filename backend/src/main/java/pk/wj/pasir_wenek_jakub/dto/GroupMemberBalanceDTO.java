package pk.wj.pasir_wenek_jakub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupMemberBalanceDTO {
    private Long userId;
    private Double balance;
}