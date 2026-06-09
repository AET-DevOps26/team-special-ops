package com.tso.chat.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_question")
public class ChatQuestion {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

  @Column(name = "question_text", nullable = false)
  private String questionText;

  @Column(name = "progress_at_ask", nullable = false)
  private int progressAtAsk;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private ChatAnswer answer;

  protected ChatQuestion() {}

  public ChatQuestion(
      UUID id,
      UUID userId,
      UUID seriesId,
      String questionText,
      int progressAtAsk,
      OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.seriesId = seriesId;
    this.questionText = questionText;
    this.progressAtAsk = progressAtAsk;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getSeriesId() {
    return seriesId;
  }

  public String getQuestionText() {
    return questionText;
  }

  public int getProgressAtAsk() {
    return progressAtAsk;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public ChatAnswer getAnswer() {
    return answer;
  }

  public void setAnswer(ChatAnswer answer) {
    this.answer = answer;
  }
}
