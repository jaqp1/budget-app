package pk.wj.pasir_wenek_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pk.wj.pasir_wenek_jakub.model.Membership;
import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    @Transactional
    void deleteByGroupId(Long groupId);

}