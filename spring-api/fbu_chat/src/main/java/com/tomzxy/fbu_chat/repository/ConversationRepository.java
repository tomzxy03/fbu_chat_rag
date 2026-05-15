package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findAllByOrderByUpdatedAtDesc();
}
