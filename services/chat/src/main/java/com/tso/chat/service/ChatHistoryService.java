package com.tso.chat.service;

import com.tso.chat.entity.ChatAnswer;
import com.tso.chat.entity.ChatQuestion;
import com.tso.chat.model.ChatAnswerResponse;
import com.tso.chat.repository.ChatQuestionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatHistoryService {

  private final ChatQuestionRepository questionRepository;

  public ChatHistoryService(ChatQuestionRepository questionRepository) {
    this.questionRepository = questionRepository;
  }

  @Transactional
  public ChatAnswerResponse saveQuestionAndAnswer(
      UUID userId,
      UUID seriesId,
      String questionText,
      int progress,
      String answerText,
      List<Integer> citedEpisodeIndices,
      OffsetDateTime createdAt) {
    UUID questionId = UUID.randomUUID();
    ChatQuestion question =
        new ChatQuestion(questionId, userId, seriesId, questionText, progress, createdAt);
    ChatAnswer answer =
        new ChatAnswer(UUID.randomUUID(), question, answerText, citedEpisodeIndices, createdAt);
    question.setAnswer(answer);
    questionRepository.save(question);

    return new ChatAnswerResponse()
        .id(questionId)
        .question(questionText)
        .answer(answerText)
        .citedEpisodeIndices(citedEpisodeIndices)
        .progressAtAsk(progress)
        .createdAt(createdAt);
  }
}
