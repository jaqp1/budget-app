package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.wj.pasir_wenek_jakub.dto.GroupResponseDTO;
import pk.wj.pasir_wenek_jakub.dto.MembershipDTO;
import pk.wj.pasir_wenek_jakub.dto.MembershipResponseDTO;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.model.Membership;
import pk.wj.pasir_wenek_jakub.model.User;
import pk.wj.pasir_wenek_jakub.repository.GroupRepository;
import pk.wj.pasir_wenek_jakub.service.CurrentUserService;
import pk.wj.pasir_wenek_jakub.service.MembershipService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MembershipGraphQLController {
    private final MembershipService membershipService;
    private final GroupRepository groupRepository;
    private final CurrentUserService currentUserService;

    public MembershipGraphQLController(
            MembershipService membershipService,
            GroupRepository groupRepository,
            CurrentUserService currentUserService) {
        this.membershipService = membershipService;
        this.groupRepository = groupRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Zwraca listę członków grupy przekształconą na MembershipResponseDTO.
     */
    @QueryMapping
    public List<MembershipResponseDTO> groupMembers(@Argument Long groupId) {
        return membershipService.getGroupMembers(groupId).stream()
                .map(membership -> new MembershipResponseDTO(
                        membership.getId(),
                        membership.getUser().getId(),
                        membership.getGroup().getId(),
                        membership.getUser().getEmail()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Dodaje nowego członka do grupy i zwraca jego dane w formacie DTO.
     */
    @MutationMapping
    public MembershipResponseDTO addMember(@Valid @Argument MembershipDTO membershipDTO) {
        Membership membership = membershipService.addMember(membershipDTO);
        return new MembershipResponseDTO(
                membership.getId(),
                membership.getUser().getId(),
                membership.getGroup().getId(),
                membership.getUser().getEmail()
        );
    }

    /**
     * Zwraca listę grup aktualnie zalogowanego użytkownika jako GroupResponseDTO.
     */
    @QueryMapping
    public List<GroupResponseDTO> myGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser).stream()
                .map(group -> new GroupResponseDTO(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getId()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Usuwa członka z grupy na podstawie identyfikatora członkostwa.
     */
    @MutationMapping
    public Boolean removeMember(@Argument Long membershipId) {
        membershipService.removeMember(membershipId);
        return true;
    }
}