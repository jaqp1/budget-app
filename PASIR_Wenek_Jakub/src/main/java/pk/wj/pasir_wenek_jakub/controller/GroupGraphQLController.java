package pk.wj.pasir_wenek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.wj.pasir_wenek_jakub.dto.GroupDTO;
import pk.wj.pasir_wenek_jakub.model.Group;
import pk.wj.pasir_wenek_jakub.service.GroupService;

import java.util.List;

@Controller
public class GroupGraphQLController {
    private final GroupService groupService;

    public GroupGraphQLController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * Mapowanie zapytania 'groups' zwracającego listę wszystkich grup użytkownika.
     */
    @QueryMapping
    public List<Group> groups() {
        return groupService.getAllGroups();
    }

    /**
     * Mapowanie mutacji 'createGroup' do tworzenia nowej grupy.
     * Wykorzystuje adnotację @Valid do walidacji danych wejściowych z GroupDTO.
     */
    @MutationMapping
    public Group createGroup(@Valid @Argument GroupDTO groupDTO) {
        return groupService.createGroup(groupDTO);
    }

    /**
     * Mapowanie mutacji 'deleteGroup' do usuwania grupy o wskazanym identyfikatorze.
     */
    @MutationMapping
    public Boolean deleteGroup(@Argument Long id) {
        groupService.deleteGroup(id);
        return true;
    }
}