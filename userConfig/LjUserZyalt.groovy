import com.gmail.kompotik.ljcrawler.LJUser

class LjUserZyalt implements LJUser {
  @Override
  String getName() {
    return "zyalt"
  }

  @Override
  public String getXpathPreviousPageLink() {
    return "//html:ul[@class ='page-nav']/html:li[@class = 'prev']/html:a"
  }

  @Override
  public String getXpathPageCounter() {
    return "//html:div[@class = 'entry-comments-text']/html:table/html:tr[1]/html:td/html:b"
  }

  @Override
  public String getXpathCommentText() {
    return "//html:div[@class = 'comment-text']"
  }

  @Override
  public String getXpathCommentUsername() {
    return "//html:span[contains(@class, 'ljuser')]//html:a[position() = 2]/html:b"
  }

  @Override
  public String getXpathCommentParent() {
    return "//html:div[@class = 'comment-menu']//html:a[text() = 'Parent']"
  }

  @Override
  public String getXpathCommentDate() {
    return "//html:div[@class = 'comment-head-in']//html:a[@class = 'comment-permalink']/html:span"
  }

  @Override
  public String postProcessCommentText(String commentText) {
    return commentText;
  }

  @Override
  public String getDateFormatForStory() {
    return "YYYY-MM-dd'T'HH:mm:ssZZ"
  }

  @Override
  public String getXpathCommentsWrap() {
    return "//html:div[starts-with(@id,'" + DIV_COMMENT_IDENTIFICATOR + "') and @style='margin-left:0px;']"
  }

  @Override
  public String getXpathStoryDate() {
    return "//html:dd[@class = 'entry-date']/html:abbr[@class = 'updated']/@title"
  }

  @Override
  public String getXpathTags() {
    return "//html:div[@class = 'entry-content']/html:div[@class = 'ljtags']/html:a"
  }

  @Override
  public Boolean supportsTags() {
    return true
  }

  @Override
  public String getXpathLinkToStory() {
    return "//html:dt[@class = 'entry-title']/html:a"
  }

  @Override
  public String getXpathH1() {
    return "//html:dt[@class = 'entry-title' and not(@class = 'entry-linkbar')]"
  }

  @Override
  public String getXpathEntryContent() {
    return "//html:div[@class = 'entry-content']"
  }

  @Override
  String postProcessEntryText(String entryText, String entryTitle) {
    return entryText
  }
}