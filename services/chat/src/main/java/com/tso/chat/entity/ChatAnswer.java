package com.tso.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_answer")
public class ChatAnswer {

  @Id private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private ChatQuestion question;

  @Column(name = "answer_text", nullable = false)
  private String answerText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "cited_episode_indices", nullable = false)
  private List<Integer> citedEpisodeIndices;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  protected ChatAnswer() {}

  public ChatAnswer(
      UUID id,
      ChatQuestion question,
      String answerText,
      List<Integer> citedEpisodeIndices,
      OffsetDateTime createdAt) {
    this.id = id;
    this.question = question;
    this.answerText = answerText;
    this.citedEpisodeIndices = citedEpisodeIndices;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public ChatQuestion getQuestion() {
    return question;
  }

  public String getAnswerText() {
    return answerText;
  }

  public List<Integer> getCitedEpisodeIndices() {
    return citedEpisodeIndices;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
