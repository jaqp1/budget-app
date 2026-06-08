package pk.wj.pasir_wenek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.wj.pasir_wenek_jakub.dto.GroupDTO;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.Membership;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.DebtRepository;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.repository.MembershipRepository;

import java.util.List;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Zwraca listę grup, do których należy aktualnie zalogowany użytkownik.
     */
    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    /**
     * Tworzy nową grupę, ustawia zalogowanego użytkownika jako właściciela
     * i dodaje go jako pierwszego członka grupy.
     */
    @Transactional
    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser();

        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);

        Group savedGroup = groupRepository.save(group);

        // Automatyczne dodanie właściciela do członków grupy (Membership)
        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);
        membershipRepository.save(membership);

        return savedGroup;
    }

    /**
     * Usuwa grupę o podanym ID. Tylko właściciel grupy ma uprawnienia do tej operacji.
     * Usuwa kaskadowo długi oraz członkostwa w danej grupie.
     */
    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć grupy. Grupa o ID " + id + " nie istnieje."));

        User currentUser = currentUserService.getCurrentUser();

        // Weryfikacja uprawnień właściciela[cite: 1]
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć.");
        }

        // Usuwanie powiązanych danych przed usunięciem samej grupy[cite: 1]
        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}