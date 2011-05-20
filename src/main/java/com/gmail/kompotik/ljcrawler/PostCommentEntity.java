package com.gmail.kompotik.ljcrawler;

import org.joda.time.DateTime;

public class PostCommentEntity {
  private Long id;
  private DateTime created;
  private String content;
  private Long localUserId;
  private Long storyId;
  private Long parentId;
  private boolean deleted;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DateTime getCreated() {
    return created;
  }

  public void setCreated(DateTime created) {
    this.created = created;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getLocalUserId() {
    return localUserId;
  }

  public void setLocalUserId(Long localUserId) {
    this.localUserId = localUserId;
  }

  public Long getStoryId() {
    return storyId;
  }

  public void setStoryId(Long storyId) {
    this.storyId = storyId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
