package com.gmail.kompotik.ljcrawler;

public enum LjUser {
  zyalt {
    @Override
    public String getXpathPreviousPageLink() {
      return "//html:ul[@class ='page-nav']/html:li[@class = 'prev']/html:a";
    }

    @Override
    public String getXpathPageCounter() {
      return "//html:div[@class = 'entry-comments-text']/html:table/html:tr[1]/html:td/html:b";
    }

    @Override
    public String getXpathCommentText() {
      return "//html:div[@class = 'comment-text']";
    }

    @Override
    public String getXpathCommentUsername() {
      return "//html:span[contains(@class, 'ljuser')]//html:a[position() = 2]/html:b";
    }

    @Override
    public String getXpathCommentParent() {
      return "//html:div[@class = 'comment-menu']//html:a[text() = 'Parent']";
    }

    @Override
    public String getXpathCommentDate() {
      return "//html:div[@class = 'comment-head-in']//html:a[@class = 'comment-permalink']/html:span";
    }

    @Override
    public String postProcessCommentText(String commentText) {
      return commentText;
    }

    @Override
    public String getDateFormatForStory() {
      return "YYYY-MM-dd'T'HH:mm:ssZZ";
    }

    @Override
    public String getXpathCommentsWrap() {
      return "//html:div[starts-with(@id,'" + DIV_COMMENT_IDENTIFICATOR + "') and @style='margin-left:0px;']";
    }

    @Override
    public String getXpathStoryDate() {
      return "//html:dd[@class = 'entry-date']/html:abbr[@class = 'updated']/@title";
    }

    @Override
    public String getXpathTags() {
      return "//html:div[@class = 'entry-content']/html:div[@class = 'ljtags']/html:a";
    }

    @Override
    public boolean supportsTags() {
      return true;
    }

    @Override
    public String getXpathLinkToStory() {
      return "//html:dt[@class = 'entry-title']/html:a";
    }

    @Override
    public String getXpathH1() {
      return "//html:dt[@class = 'entry-title' and not(@class = 'entry-linkbar')]";
    }

    @Override
    public String getXpathEntryContent() {
      return "//html:div[@class = 'entry-content']";
    }
  },
  drugoi {
    @Override
    public String getXpathPreviousPageLink() {
      return "//html:table[1]/html:tr[1]/html:td[2]/html:table[1]/html:tr[5]/html:td[1]/html:a[1]";
    }

    @Override
    public String getXpathPageCounter() {
      return "//html:div[@id = 'Comments']//html:div[@class = 'standout']//html:td[@colspan = '3']";
    }

    @Override
    public String getXpathCommentUsername() {
      return "//html:span[contains(@class, 'ljuser')]//html:a[position() = 2]/html:b";
    }

    @Override
    public String getXpathCommentParent() {
      return "//html:div[@class = 'talk-comment-box']//html:a[text() = 'Parent']";
    }

    @Override
    public String getXpathCommentDate() {
      return "//html:div[@class = 'talk-comment-head']//html:font[2]";
    }

    @Override
    public String postProcessCommentText(String commentText) {
      final String garbageStart = "<p style=\"margin: 0.7em 0 0.2em 0\">";
      if (commentText.contains(garbageStart)) {
        return commentText.substring(0, commentText.indexOf(garbageStart));
      } else {
        return commentText;
      }
    }

    @Override
    public String getXpathCommentText() {
      return "//html:div[@class = 'talk-comment-box']";
    }

    @Override
    public String getDateFormatForStory() {
      return "'@ 'YYYY-MM-dd HH:mm:ss";
    }

    @Override
    public String getXpathCommentsWrap() {
      return "//html:div[@id = 'Comments']//html:div[starts-with(@id,'" + DIV_COMMENT_IDENTIFICATOR + "') and html:table[@class = 'talk-comment']/html:tr[1]/html:td[1]/html:img[@width = '0']]";
    }

    @Override
    public String getXpathTags() {
      return null;
    }

    @Override
    public String getXpathStoryDate() {
      return "//html:div[@id = 'content-wrapper']//html:p[1]/html:table[1]//html:td[2]/html:font";
    }

    @Override
    public String getXpathLinkToStory() {
      return "//html:td[@bgcolor = '#E0E0E0']/html:a";
    }

    @Override
    public String getXpathH1() {
      return "//html:div[@id = 'content-wrapper']//html:div[@style = 'margin-left: 30px']//html:font[position() = 1]/html:i/html:b";
    }

    @Override
    public String getXpathEntryContent() {
      return "//html:div[@id = 'content-wrapper']//html:div[@style = 'margin-left: 30px']";
    }

    @Override
    public boolean supportsTags() {
      return false;
    }
  };

  public abstract String getXpathLinkToStory();

  public abstract String getXpathH1();

  public abstract String getXpathEntryContent();

  public abstract boolean supportsTags();

  public abstract String getXpathTags();

  public abstract String getXpathStoryDate();

  public abstract String getDateFormatForStory();

  public abstract String getXpathCommentsWrap();

  public final static String DIV_COMMENT_IDENTIFICATOR = "ljcmt";

  public abstract String getXpathCommentText();

  public abstract String getXpathCommentUsername();

  public abstract String getXpathCommentParent();

  public abstract String getXpathCommentDate();

  public abstract String postProcessCommentText(String commentText);

  public abstract String getXpathPageCounter();

  public abstract String getXpathPreviousPageLink();
}
