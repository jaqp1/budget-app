package pk.wj.pasir_wenek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.wj.pasir_wenek_jakub.dto.GroupTransactionDTO;
import pk.wj.pasir_wenek_jakub.dto.GroupNotificationDTO; // Dodany import DTO powiadomień
import pk.wj.pasir_wenek_jakub.model.Debt;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.Membership;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.DebtRepository;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.repository.MembershipRepository;
import pk.wj.pasir_wenek_jakub.websocket.NotificationWebSocketHandler; // Dodany import handlera

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class GroupTransactionService {
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final NotificationWebSocketHandler notificationWebSocketHandler; // Dodane pole handlera

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            NotificationWebSocketHandler notificationWebSocketHandler) { // Dodany parametr do konstruktora
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.notificationWebSocketHandler = notificationWebSocketHandler; // Inicjalizacja handlera
    }

    /**
     * Rozdziela kwotę transakcji na wszystkich członków grupy i tworzy długi.
     */
    @Transactional
    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        // 1. Pobieramy pełny obiekt grupy na podstawie groupId z DTO
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Grupa o podanym ID nie istnieje"));

        // 2. Pobieramy wszystkich członków grupy z bazy danych
        List<Membership> members = membershipRepository.findByGroupId(group.getId());

        // 3. Filtrujemy tylko tych, którzy zostali wybrani w DTO
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Lista uczestników nie może być pusta");
        }

        // 4. Obliczamy kwotę przypadającą na jednego użytkownika
        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        // 5. Tworzymy i zapisujemy długi dla poszczególnych osób
        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group); // Teraz zmienna 'group' jest już poprawnie zdefiniowana
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());

                debtRepository.save(debt);

                // =========================================================================
                // IMPLEMENTACJA ZADANIA 5: POWIADOMIENIA WEBSOCKET W CZASIE RZECZYWISTYM
                // =========================================================================
                if (expense) { // System wysyła komunikat, gdy inny członek doda wydatek grupowy
                    String formattedMessage = String.format("%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                            currentUser.getEmail(), transactionDTO.getTitle(), group.getName(), amountPerUser);

                    GroupNotificationDTO notification = GroupNotificationDTO.builder()
                            .groupId(group.getId())
                            .groupName(group.getName())
                            .title(transactionDTO.getTitle())
                            .amount(transactionDTO.getAmount())
                            .userShare(amountPerUser)
                            .createdByEmail(currentUser.getEmail())
                            .message(formattedMessage)
                            .build();

                    // Wysyłamy wiadomość bezpośrednio do powiązanego użytkownika
                    notificationWebSocketHandler.sendNotificationToUser(otherUser.getEmail(), notification);
                }
                // =========================================================================
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }
        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();
        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }
        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }
        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }
        return selectedMembers;
    }

}