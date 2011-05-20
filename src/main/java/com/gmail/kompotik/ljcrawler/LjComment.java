package com.gmail.kompotik.ljcrawler;

import org.joda.time.DateTime;

public class LjComment {
  public String thread;
  // this field is to be set by gson only and should not be emitted by snakeyaml
  // this is intended
  private String html;
  public int depth;
  public String state;
  public String parent;
  public DateTime date;
  public String text;
  public String user;

  public boolean shouldExpand() {
    return getState().equals("collapsed");
  }

  public boolean hasBeenDeleted() {
    return getHtml().contains("Deleted post") || getHtml().contains("Deleted comment");
  }

  public boolean isAnonymous() {
    return getHtml().contains("<em>(Anonymous)</em>");
  }

   public boolean isAccountSuspended() {
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
        .append("; date =").append(getDate())
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

  public DateTime getDate() {
    return date;
  }

  public void setDate(DateTime date) {
    this.date = date;
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
}
