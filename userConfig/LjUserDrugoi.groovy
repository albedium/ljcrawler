import com.gmail.kompotik.ljcrawler.LJUser

class LjUserDrugoi implements LJUser {
  @Override
  String getName() {
    return "drugoi"
  }

  @Override
  public String getXpathPreviousPageLink() {
    return "//html:table[1]/html:tr[1]/html:td[2]/html:table[1]/html:tr[5]/html:td[1]/html:a[1]"
  }

  @Override
  public String getXpathPageCounter() {
    return "//html:div[@id = 'Comments']//html:div[@class = 'standout']//html:td[@colspan = '3']"
  }

  @Override
  public String getXpathCommentUsername() {
    return "//html:span[contains(@class, 'ljuser')]//html:a[position() = 2]/html:b"
  }

  @Override
  public String getXpathCommentParent() {
    return "//html:div[@class = 'talk-comment-box']//html:a[text() = 'Parent']"
  }

  @Override
  public String getXpathCommentDate() {
    return "//html:div[@class = 'talk-comment-head']//html:font[2]"
  }

  @Override
  public String postProcessCommentText(String commentText) {
    String garbageStart = "<p style=\"margin: 0.7em 0 0.2em 0\">"
    if (commentText.contains(garbageStart)) {
      return commentText.substring(0, commentText.indexOf(garbageStart))
    } else {
      return commentText
    }
  }

  @Override
  String postProcessEntryText(String entryText, String entryTitle) {
    String text = '<font face="Arial,Helvetica" size="+1"><i><b>' + entryTitle + '</b></i></font><br />'
    if (entryText.startsWith(text)) {
      return entryText.substring(text.length());
    }
    return entryText
  }

  @Override
  public String getXpathCommentText() {
    return "//html:div[@class = 'talk-comment-box']"
  }

  @Override
  public String getDateFormatForStory() {
    return "'@ 'YYYY-MM-dd HH:mm:ss"
  }

  @Override
  public String getXpathCommentsWrap() {
    return "//html:div[@id = 'Comments']//html:div[starts-with(@id,'" + DIV_COMMENT_IDENTIFICATOR + "') and html:table[@class = 'talk-comment']/html:tr[1]/html:td[1]/html:img[@width = '0']]"
  }

  @Override
  public String getXpathTags() {
    return null
  }

  @Override
  public String getXpathStoryDate() {
    return "//html:div[@id = 'content-wrapper']//html:p[1]/html:table[1]//html:td[2]/html:font"
  }

  @Override
  public String getXpathLinkToStory() {
    return "//html:td[@bgcolor = '#E0E0E0']/html:a"
  }

  @Override
  public String getXpathH1() {
    return "//html:div[@id = 'content-wrapper']//html:div[@style = 'margin-left: 30px']//html:font[position() = 1]/html:i/html:b"
  }

  @Override
  public String getXpathEntryContent() {
    return "//html:div[@id = 'content-wrapper']//html:div[@style = 'margin-left: 30px']"
  }

  @Override
  public Boolean supportsTags() {
    return false
  }
}