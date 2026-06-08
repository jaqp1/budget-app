package pk.wj.pasir_wenek_jakub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import pk.wj.pasir_wenek_jakub.dto.DebtDTO;
import pk.wj.pasir_wenek_jakub.dto.GroupDTO;
import pk.wj.pasir_wenek_jakub.dto.GroupTransactionDTO;
import pk.wj.pasir_wenek_jakub.dto.MembershipDTO;
import pk.wj.pasir_wenek_jakub.model.Debt;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.Membership;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.DebtRepository;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.repository.MembershipRepository;
import pk.wj.pasir_wenek_jakub.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional // Gwarantuje, że po każdym teście baza danych wróci do stanu początkowego
public class BudgetBusinessLogicTest {

    @Autowired private GroupService groupService;
    @Autowired private MembershipService membershipService;
    @Autowired private DebtService debtService;
    @Autowired private GroupTransactionService transactionService;

    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private DebtRepository debtRepository;

    @MockitoBean private CurrentUserService currentUserService;

    private User owner;
    private User member1;
    private User member2;
    private User nonMember;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        // Przygotowanie użytkowników w bazie testowej
        owner = userRepository.save(new User(null, "Owner", "owner@test.com", "pass", "PLN"));
        member1 = userRepository.save(new User(null, "Member1", "member1@test.com", "pass", "PLN"));
        member2 = userRepository.save(new User(null, "Member2", "member2@test.com", "pass", "PLN"));
        nonMember = userRepository.save(new User(null, "NonMember", "nonmember@test.com", "pass", "PLN"));

        // Ustawienie właściciela jako domyślnie zalogowanego
        setCurrentUser(owner);

        // Tworzenie grupy testowej
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setName("Testowa Grupa");
        testGroup = groupService.createGroup(groupDTO);

        // Dodanie członków
        MembershipDTO m1Dto = new MembershipDTO();
        m1Dto.setGroupId(testGroup.getId());
        m1Dto.setUserEmail(member1.getEmail());
        membershipService.addMember(m1Dto);

        MembershipDTO m2Dto = new MembershipDTO();
        m2Dto.setGroupId(testGroup.getId());
        m2Dto.setUserEmail(member2.getEmail());
        membershipService.addMember(m2Dto);
    }

    private void setCurrentUser(User user) {
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    // 1. Utworzenie grupy dodaje właściciela jako członka i zwraca ją w myGroups
    @Test
    void createGroup_AddsOwnerAsMember() {
        List<Group> myGroups = groupService.getAllGroups();
        assertEquals(1, myGroups.size());
        assertEquals(owner.getId(), myGroups.get(0).getOwner().getId());

        List<Membership> members = membershipService.getGroupMembers(testGroup.getId());
        assertTrue(members.stream().anyMatch(m -> m.getUser().getId().equals(owner.getId())));
    }

    // 2. Tylko właściciel grupy może dodawać członków
    @Test
    void addMember_ByNonOwner_ThrowsException() {
        setCurrentUser(member1); // Zmieniamy zalogowanego na zwykłego członka
        MembershipDTO newMember = new MembershipDTO();
        newMember.setGroupId(testGroup.getId());
        newMember.setUserEmail(nonMember.getEmail());

        assertThrows(AccessDeniedException.class, () -> membershipService.addMember(newMember));
    }

    // 3 & 4. groupMembers i groupDebts zwraca dane tylko członkowi tej grupy
    @Test
    void getGroupData_ByNonMember_ThrowsException() {
        setCurrentUser(nonMember);
        assertThrows(AccessDeniedException.class, () -> membershipService.getGroupMembers(testGroup.getId()));
        assertThrows(AccessDeniedException.class, () -> debtService.getGroupDebts(testGroup.getId()));
    }

    // 6. Transakcja grupowa typu INCOME tworzy długi od aktualnego użytkownika do pozostałych
    @Test
    void addGroupTransaction_Income_CreatesCorrectDebts() {
        setCurrentUser(member1);
        GroupTransactionDTO tx = new GroupTransactionDTO();
        tx.setGroupId(testGroup.getId());
        tx.setAmount(300.0);
        tx.setType("INCOME");
        tx.setTitle("Wypłata grupowa");

        transactionService.addGroupTransaction(tx, member1);

        List<Debt> debts = debtService.getGroupDebts(testGroup.getId());
        // Kwota 300 / 3 członków = 100 na osobę. INCOME oznacza, że member1 ma dług wobec innych
        assertEquals(2, debts.size()); // Dwa długi (wobec owner i member2)
        assertTrue(debts.stream().allMatch(d -> d.getDebtor().getId().equals(member1.getId())));
        assertTrue(debts.stream().allMatch(d -> d.getAmount() == 100.0));
    }

    // 8. Nie można usunąć właściciela z jego grupy przez removeMember
    @Test
    void removeMember_Owner_ThrowsException() {
        setCurrentUser(owner);
        Membership ownerMembership = membershipRepository.findByGroupId(testGroup.getId()).stream()
                .filter(m -> m.getUser().getId().equals(owner.getId()))
                .findFirst().orElseThrow();

        assertThrows(IllegalStateException.class, () -> membershipService.removeMember(ownerMembership.getId()));
    }

    // 9. Członek grupy niebędący właścicielem nie może usunąć grupy
    @Test
    void deleteGroup_ByNonOwner_ThrowsException() {
        setCurrentUser(member1);
        assertThrows(AccessDeniedException.class, () -> groupService.deleteGroup(testGroup.getId()));
    }

    // 10 & 11. createDebt tworzy ręczny dług tylko między członkami grupy i odrzuca dług do samego siebie
    @Test
    void createDebt_InvalidParticipants_ThrowsException() {
        setCurrentUser(owner);
        DebtDTO debtDTO = new DebtDTO();
        debtDTO.setGroupId(testGroup.getId());
        debtDTO.setAmount(50.0);
        debtDTO.setTitle("Test");

        // Dług do samego siebie
        debtDTO.setDebtorId(owner.getId());
        debtDTO.setCreditorId(owner.getId());
        assertThrows(IllegalStateException.class, () -> debtService.createDebt(debtDTO));

        // Osoba spoza grupy
        debtDTO.setCreditorId(nonMember.getId());
        assertThrows(AccessDeniedException.class, () -> debtService.createDebt(debtDTO));
    }

    // 12. Właściciel grupy może utworzyć dług między innymi członkami grupy
    @Test
    void createDebt_ByOwnerForOthers_Success() {
        setCurrentUser(owner);
        DebtDTO debtDTO = new DebtDTO();
        debtDTO.setGroupId(testGroup.getId());
        debtDTO.setDebtorId(member1.getId());
        debtDTO.setCreditorId(member2.getId());
        debtDTO.setAmount(50.0);
        debtDTO.setTitle("Zlecony dług");

        Debt savedDebt = debtService.createDebt(debtDTO);
        assertNotNull(savedDebt.getId());
    }

    // 14 & 15. deleteDebt zachowanie uprawnień
    @Test
    void deleteDebt_Permissions() {
        // Setup: Właściciel tworzy dług między m1 i m2
        setCurrentUser(owner);
        DebtDTO debtDTO = new DebtDTO();
        debtDTO.setGroupId(testGroup.getId());
        debtDTO.setDebtorId(member1.getId());
        debtDTO.setCreditorId(member2.getId());
        debtDTO.setAmount(10.0);
        debtDTO.setTitle("Test");
        Debt debt = debtService.createDebt(debtDTO);

        // m1 (uczestnik) może usunąć
        setCurrentUser(member1);
        assertDoesNotThrow(() -> debtService.deleteDebt(debt.getId()));

        // Odtworzenie długu
        setCurrentUser(owner);
        Debt debt2 = debtService.createDebt(debtDTO);

        // owner (właściciel, nie uczestnik) może usunąć (wymóg nr 16)
        assertDoesNotThrow(() -> debtService.deleteDebt(debt2.getId()));
    }

    // 18. Usunięcie grupy przez właściciela usuwa powiązane długi i grupę
    @Test
    void deleteGroup_ByOwner_DeletesCascadingData() {
        setCurrentUser(owner);

        // Dodanie testowego długu
        DebtDTO debtDTO = new DebtDTO();
        debtDTO.setGroupId(testGroup.getId());
        debtDTO.setDebtorId(member1.getId());
        debtDTO.setCreditorId(owner.getId());
        debtDTO.setAmount(10.0);
        debtDTO.setTitle("Test");
        debtService.createDebt(debtDTO);

        // Usunięcie grupy
        groupService.deleteGroup(testGroup.getId());

        // Weryfikacja usunięcia
        assertTrue(groupRepository.findById(testGroup.getId()).isEmpty());
        assertTrue(membershipRepository.findByGroupId(testGroup.getId()).isEmpty());
        assertTrue(debtRepository.findByGroupId(testGroup.getId()).isEmpty());
    }
}