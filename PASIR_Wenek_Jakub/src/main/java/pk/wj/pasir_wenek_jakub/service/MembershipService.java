package pk.wj.pasir_wenek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.wj.pasir_wenek_jakub.dto.MembershipDTO;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.Membership;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.repository.MembershipRepository;
import pk.wj.pasir_wenek_jakub.repository.UserRepository;

import java.util.List;

@Service
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public MembershipService(
            MembershipRepository membershipRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Zwraca listę wszystkich członków danej grupy.
     */
    public List<Membership> getGroupMembers(Long groupId) {
        assertCurrentUserIsGroupMember(groupId);
        return membershipRepository.findByGroupId(groupId);
    }

    /**
     * Dodaje nowego użytkownika do grupy na podstawie e-maila.
     */
    public Membership addMember(MembershipDTO membershipDTO) {
        // Tylko właściciel grupy może dodawać członków
        assertCurrentUserIsGroupOwner(membershipDTO.getGroupId());

        User user = userRepository.findByEmail(membershipDTO.getUserEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika o emailu: " + membershipDTO.getUserEmail()));

        Group group = groupRepository.findById(membershipDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + membershipDTO.getGroupId()));

        // Walidacja: sprawdzenie czy użytkownik już jest w grupie
        boolean alreadyMember = membershipRepository.findByGroupId(group.getId()).stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()));

        if (alreadyMember) {
            throw new IllegalStateException("Użytkownik jest już członkiem tej grupy.");
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);
        return membershipRepository.save(membership);
    }

    /**
     * Usuwa członka z grupy. Tylko właściciel może usuwać innych[cite: 1].
     */
    public void removeMember(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Członkostwo nie istnieje"));

        User currentUser = currentUserService.getCurrentUser();
        User groupOwner = membership.getGroup().getOwner();

        if (!currentUser.getId().equals(groupOwner.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może usuwać członków.");
        }

        // Blokada usunięcia samego siebie, jeśli jest się właścicielem[cite: 1]
        if (membership.getUser().getId().equals(groupOwner.getId())) {
            throw new IllegalStateException("Nie można usunąć właściciela z jego grupy.");
        }

        membershipRepository.delete(membership);
    }

    // --- Metody pomocnicze do weryfikacji uprawnień ---

    public void assertCurrentUserIsGroupMember(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o ID: " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        assertUserIsGroupMember(groupId, currentUser.getId());
    }

    public void assertCurrentUserIsGroupOwner(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o ID: " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może wykonać tę operację.");
        }
    }

    public void assertUserIsGroupMember(Long groupId, Long userId) {
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AccessDeniedException("Użytkownik nie jest członkiem tej grupy.");
        }
    }
}