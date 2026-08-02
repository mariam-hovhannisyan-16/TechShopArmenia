package am.techshop.chat.repository;

import am.techshop.chat.entity.Message;
import am.techshop.common.enums.MessageSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    boolean existsByConversationIdAndSender(Long conversationId, MessageSender sender);

    void deleteByConversationIdIn(List<Long> conversationIds);
}
