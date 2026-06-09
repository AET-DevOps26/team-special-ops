package com.tso.chat.repository;

import com.tso.chat.entity.ChatQuestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatQuestionRepository extends JpaRepository<ChatQuestion, UUID> {}
