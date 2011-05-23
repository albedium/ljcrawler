package com.gmail.kompotik.ljcrawler;

public interface LJUser {
  String getName();

  String getXpathLinkToStory();

  String getXpathH1();

  String getXpathEntryContent();

  Boolean supportsTags();

  String getXpathTags();

  String getXpathStoryDate();

  String getDateFormatForStory();

  String getXpathCommentsWrap();

  public final static String DIV_COMMENT_IDENTIFICATOR = "ljcmt";

  String getXpathCommentText();

  String getXpathCommentUsername();

  String getXpathCommentParent();

  String getXpathCommentDate();

  String postProcessCommentText(String commentText);

  String postProcessEntryText(String entryText, String entryTitle);

  String getXpathPageCounter();

  String getXpathPreviousPageLink();
}
