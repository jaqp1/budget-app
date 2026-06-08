package pk.wj.pasir_wenek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.wj.pasir_wenek_jakub.dto.DebtDTO;
import pk.wj.pasir_wenek_jakub.model.Debt;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.DebtRepository;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtService {
    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;

    public DebtService(
            DebtRepository debtRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            MembershipService membershipService,
            CurrentUserService currentUserService) {
        this.debtRepository = debtRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
    }

    /**
     * Zwraca listę długów dla danej grupy, sprawdzając czy użytkownik jest jej członkiem.
     */
    public List<Debt> getGroupDebts(Long groupId) {
        membershipService.assertCurrentUserIsGroupMember(groupId);
        return debtRepository.findByGroupId(groupId);
    }

    /**
     * Tworzy nowy dług na podstawie DebtDTO z walidacją uczestników i uprawnień.
     */
    public Debt createDebt(DebtDTO debtDTO) {
        Group group = groupRepository.findById(debtDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Grupa o ID " + debtDTO.getGroupId() + " nie istnieje."));

        User debtor = userRepository.findById(debtDTO.getDebtorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Dłużnik o ID " + debtDTO.getDebtorId() + " nie istnieje."));

        User creditor = userRepository.findById(debtDTO.getCreditorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Wierzyciel o ID " + debtDTO.getCreditorId() + " nie istnieje."));

        // Walidacja członkostwa w grupie
        membershipService.assertCurrentUserIsGroupMember(group.getId());
        membershipService.assertUserIsGroupMember(group.getId(), debtor.getId());
        membershipService.assertUserIsGroupMember(group.getId(), creditor.getId());

        // Sprawdzenie czy dłużnik i wierzyciel to nie ta sama osoba
        if (debtor.getId().equals(creditor.getId())) {
            throw new IllegalStateException("Dłużnik i wierzyciel muszą być różnymi użytkownikami.");
        }

        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(group, debtor, creditor, currentUser);

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());

        return debtRepository.save(debt);
    }

    /**
     * Usuwa wskazany dług po weryfikacji uprawnień[cite: 1].
     */
    public void deleteDebt(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć długu. Dług o ID " + debtId + " nie istnieje."));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(debt.getGroup(), debt.getDebtor(), debt.getCreditor(), currentUser);

        debtRepository.delete(debt);
    }

    /**
     * Sprawdza, czy użytkownik ma prawo zarządzać długiem (właściciel grupy lub uczestnik długu)[cite: 1].
     */
    private void assertCurrentUserCanManageDebt(Group group, User debtor, User creditor, User currentUser) {
        boolean isGroupOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isDebtParticipant = debtor.getId().equals(currentUser.getId())
                || creditor.getId().equals(currentUser.getId());

        if (!isGroupOwner && !isDebtParticipant) {
            throw new AccessDeniedException(
                    "Tylko właściciel grupy albo uczestnik długu może wykonać tę operację.");
        }
    }
    public Debt markDebtAsPaid(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();
        if (!debt.getDebtor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko dluznik moze oznaczyc dlug jako oplacony.");
        }
        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);
        return debtRepository.save(debt);
    }
    public Debt confirmDebtPayment(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();
        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko wierzyciel moze potwierdzic splate dlugu.");
        }

        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException(
                    "Dlug musi zostac najpierw oznaczony jako oplacony przez dluznika.");
        }
        debt.setConfirmedByCreditor(true);
        return debtRepository.save(debt);
    }
    private Debt getDebtForCurrentGroupMember(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono dlugu o ID " + debtId + "."));
        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        return debt;
    }

    public List<Debt> getDebtsForBalance(Long groupId, LocalDate startDate, LocalDate endDate) {
        // 1. Filtrowanie po czasie (Krok 4 z naszej listy kroków)
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59, 999999999);

            return debtRepository.findByGroupIdAndCreatedAtBetween(groupId, start, end);
        }

        // Brak filtrów – pobieramy wszystko dla grupy
        return debtRepository.findByGroupId(groupId);
    }

    public Map<Long, Double> calculateGroupBalance(Long groupId, LocalDate startDate, LocalDate endDate) {
        // 1. Pobieramy długi za pomocą metody z filtrem czasowym (tej, którą pisałeś w DebtService)
        List<Debt> debts = getDebtsForBalance(groupId, startDate, endDate);

        // Mapa: Key = ID Użytkownika, Value = Jego aktualny bilans
        Map<Long, Double> balanceMap = new HashMap<>();

        // 2. Przeliczamy dynamicznie każdy dług na bilans
        for (Debt debt : debts) {
            Long debtorId = debt.getDebtor().getId();
            Long creditorId = debt.getCreditor().getId();
            Double amount = debt.getAmount();

            // Dłużnik (debtor) wychodzi na minus
            balanceMap.put(debtorId, balanceMap.getOrDefault(debtorId, 0.0) - amount);

            // Wierzyciel (creditor) wychodzi na plus
            balanceMap.put(creditorId, balanceMap.getOrDefault(creditorId, 0.0) + amount);
        }

        return balanceMap;
    }

}