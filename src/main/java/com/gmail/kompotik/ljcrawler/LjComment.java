package com.gmail.kompotik.ljcrawler;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

public class LjComment {
  public String thread;
  // this field is to be set by gson only and should not be emitted by snakeyaml
  // this is intended
  private String html;
  public int depth;
  public String state;
  public String parent;
  public Date date;
  public String text;
  public String user;
  public String postId;

  public boolean shouldExpand() {
    return getState().equals("collapsed");
  }

  public boolean hasBeenDeleted() {
    if (StringUtils.isBlank(getHtml())) {
      // is that right; may be `true` should be returned
      return false;
    }
    return getHtml().contains("Deleted post") || getHtml().contains("Deleted comment");
  }

  public boolean isAnonymous() {
    if (StringUtils.isBlank(getHtml())) {
      return false;
    }
    return getHtml().contains("<em>(Anonymous)</em>");
  }

  public boolean isAccountSuspended() {
    if (StringUtils.isBlank(getHtml())) {
      return false;
    }
    return getHtml().contains("text-decoration:line-through");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LjComment that = (LjComment) o;

    if (getThread() != null ? !getThread().equals(that.getThread()) : that.getThread() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getThread() != null ? getThread().hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringBuffer()
        .append("thread =").append(getThread())
        .append("; html =").append(getHtml())
        .append("; depth =").append(getDepth())
        .append("; state =").append(getState())
        .append("; parent =").append(getParent())
//        .append("; date =").append(getDate())
        .append("; text =").append(getText())
        .append("; user =").append(getUser())
    .toString();
  }

  public String getThread() {
    return thread;
  }

  public void setThread(String thread) {
    this.thread = thread;
  }

  public String getHtml() {
    return html;
  }

//  public void setHtml(String html) {
//    this.html = html;
//  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date dateTime) {
    this.date = dateTime;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }
}
