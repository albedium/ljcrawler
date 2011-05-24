package com.gmail.kompotik.ljcrawler;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

public class LjPostTest {
  private Long id;
  private String name;
  private String content;
  private Long authorId;
  private Long parentId;
  private Long lastStoryId;
  private Date date;
  private List<String> tags = Lists.newArrayList();
  private String href;
  private String postId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long authorId) {
    this.authorId = authorId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Long getLastStoryId() {
    return lastStoryId;
  }

  public void setLastStoryId(Long lastStoryId) {
    this.lastStoryId = lastStoryId;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }
}
