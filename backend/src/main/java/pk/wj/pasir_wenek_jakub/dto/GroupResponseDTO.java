package pk.wj.pasir_wenek_jakub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponseDTO {

    private Long id;
    private String name;
    private Long ownerId;

}