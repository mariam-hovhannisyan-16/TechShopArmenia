package am.techshop.chat.repository;

import am.techshop.chat.entity.Conversation;
import am.techshop.common.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, ConversationStatus status);

    Optional<Conversation> findFirstByGuestSessionIdAndStatusOrderByIdDesc(String guestSessionId, ConversationStatus status);

    List<Conversation> findAllByOrderByIdDesc();

    List<Conversation> findByUserId(Long userId);
}
