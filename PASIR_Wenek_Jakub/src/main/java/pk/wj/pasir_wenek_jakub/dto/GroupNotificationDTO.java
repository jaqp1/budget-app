package pk.wj.pasir_wenek_jakub.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupNotificationDTO {
    private final String type = "GROUP_EXPENSE_ADDED";
    private Long groupId;
    private String groupName;
    private String title;
    private Double amount;
    private Double userShare;
    private String createdByEmail;
    private String message;
}