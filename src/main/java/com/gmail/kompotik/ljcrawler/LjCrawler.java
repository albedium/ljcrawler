package com.gmail.kompotik.ljcrawler;

import org.apache.commons.lang.StringUtils;
import org.apache.xalan.xsltc.trax.SAX2DOM;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.ccil.cowan.tagsoup.Parser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.gmail.kompotik.ljcrawler.snakeyaml.JodaTimeRepresenter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class LjCrawler {
  private final UrlFetcher urlFetcher;
  private LjUser ljUser;
  private String processedDir;

  @Inject
  public LjCrawler(@Named("ljcrawler.dir.processed") String processedDir, UrlFetcher urlFetcher) {
    this.processedDir = processedDir;
    this.urlFetcher = urlFetcher;
  }

  public void crawl(LjUser ljUser) throws Exception {
    this.ljUser = ljUser;
    doCrawl("http://" + this.ljUser.name() + ".livejournal.com/");
  }

  private void doCrawl(String urlToSearchArticlesAt) throws Exception {
    final byte[] bytes = urlFetcher.urlToByteArray(urlToSearchArticlesAt);
    if (bytes == null) {
      return;
    }

    Node doc = getDocument(new String(bytes));


    XObject titles = XPathAPI.eval(doc, ljUser.getXpathLinkToStory());
    LjPost ljPostParent = null;
    for (int i = 0; i < titles.nodelist().getLength(); i++) {
      final Node item = titles.nodelist().item(i);
      final String storyUrl = item.getAttributes().getNamedItem("href").getTextContent();
      System.out.println("!!!! " + item.getTextContent() + " - " + storyUrl);
      final LjPost ljPost = fetchAndSaveArticle(storyUrl, ljPostParent);
      if (i == 0) {
        ljPostParent = ljPost;
      }
    }


    XObject prevLink = XPathAPI.eval(doc, ljUser.getXpathPreviousPageLink());
    if (prevLink.nodelist() != null) {
      final Node prevUrl = prevLink.nodelist().item(0).getAttributes().getNamedItem("href");
      System.out.println("============ end of page ===========");
      System.out.println("========= starting new topic =======");
      System.out.println("previous articles bunch will be loaded from " + prevUrl.getTextContent());
//      doCrawl(prevUrl.getTextContent());
    }
  }

  private LjPost fetchAndSaveArticle(String storyUrl, LjPost parent) throws Exception {
    final byte[] bytes = urlFetcher.urlToByteArray(storyUrl);
    if (bytes == null) {
      return null;
    }
    Node doc = getDocument(new String(bytes));
    LjPost ljPost = new LjPost();
    final XObject h1 = XPathAPI.eval(doc, ljUser.getXpathH1());
    ljPost.setName(h1.nodelist().item(0).getTextContent());
    System.out.println("-- h1 = " + h1.nodelist().item(0).getTextContent());
    String postId = storyUrl.substring(storyUrl.lastIndexOf('/') + 1, storyUrl.lastIndexOf('.'));
    System.out.println("-- id = " + postId);
    ljPost.setPostId(postId);
    final XObject entryContent = XPathAPI.eval(doc, ljUser.getXpathEntryContent());
    final String dirtyContent = stripOuterMostTag(xobjectToString(entryContent));
    String cleanFirstPhase = unsafeHtmlClean(dirtyContent);
    String cleanSecondPhase = safeHtmlClean(stripOuterMostTag(nodeToString(getDocument(cleanFirstPhase).getFirstChild().getFirstChild())));
    if (ljUser.supportsTags()) {
      final XObject tagsXObject = XPathAPI.eval(doc, ljUser.getXpathTags());
      List<String> tags = getTags(tagsXObject);
      System.out.println("-- tags are ");
      System.out.println();
      for (String tag : tags) {
        System.out.println("-  " + tag);
      }
    }
    final XObject imagesXObject = XPathAPI.eval(doc, ljUser.getXpathEntryContent() + "//html:img");
    cleanSecondPhase = persistImages(imagesXObject, cleanSecondPhase);
    System.out.println("-- after image saving = " + cleanSecondPhase);
    ljPost.setContent(cleanSecondPhase);
//    ljPost.setStatus(Story.Status.PUBLISHED);
    final XObject date = XPathAPI.eval(doc, ljUser.getXpathStoryDate());
    System.out.println("date = " + date.nodelist().item(0).getTextContent());
    DateTimeFormatter fmt = DateTimeFormat.forPattern(ljUser.getDateFormatForStory());
    final DateTime dateTime = fmt.parseDateTime(date.nodelist().item(0).getTextContent());
    /**
     * тут некоторая фигня с датами
     * date = 2011-05-13T15:47:00+03:00
     * parsed date = 2011-05-13T16:47:00.000+04:00
     * formatted date = 2011-05-13 16:47:00
     */
    ljPost.setDate(dateTime);
    System.out.println("parsed date = " + dateTime);
    System.out.println("formatted date = " + dateTime.toString("YYYY-MM-dd HH:mm:ss"));
    System.out.println();
    System.out.println();
    ljPost.setParentId(parent != null ? parent.getId() : null);

    processComments(doc, postId, ljPost);
    final XObject totalPages = XPathAPI.eval(doc, ljUser.getXpathPageCounter());
    if (StringUtils.isNotBlank(xobjectToString(totalPages))) {
      final String totalPagesString = stripOuterMostTag(xobjectToString(totalPages));
      final int totalPagesInt = Integer.parseInt(totalPagesString.substring(totalPagesString.lastIndexOf(" ") + 1));
      System.out.println("-- total comment pages found = " + totalPagesInt);
      if (totalPagesInt > 1) {
        for (int i = 2; i <= totalPagesInt; i++) {
          final byte[] bytesComments = urlFetcher.urlToByteArray(storyUrl + "?page=" + i);
          if (bytesComments == null) {
            continue;
          }
          final Node commentsDoc = getDocument(new String(bytesComments));
          processComments(
              commentsDoc,
              postId,
              ljPost);
        }
      }
    } else {
      System.out.println("-- no additional comment pages found");
    }

    ljPost = dumpPostToDisk(ljPost);

    return ljPost;
  }

  private void processComments(Node doc, String postId, LjPost ljPost) throws Exception {
    // загрузим все комменты первого уровня, а потом будет делать по одному запросу на каждую ветку
    // и парсить уже всю ветку
    final XObject commentsTopLevel = XPathAPI.eval(doc, ljUser.getXpathCommentsWrap());
    System.out.println("-- top level comment count = " + commentsTopLevel.nodelist().getLength());
    Map<String, Long> dbIdByThread = Maps.newHashMap();
    for (int i = 0; i < commentsTopLevel.nodelist().getLength(); i++) {
      final Node commentNode = commentsTopLevel.nodelist().item(i);
      final String threadId = commentNode.getAttributes().getNamedItem("id").getTextContent()
          .substring(LjUser.DIV_COMMENT_IDENTIFICATOR.length()
          );
      List<LjComment> allCommentsExpanded = new ArrayList<LjComment>();
      loadCommentByThread(postId, threadId, allCommentsExpanded, 0);
      ljPost.getLjComments().addAll(allCommentsExpanded);
      for (LjComment ljComment : allCommentsExpanded) {
        preProcessForSavingAndPrintLjComment(ljComment, allCommentsExpanded);
        saveComment(ljComment, dbIdByThread, ljPost);
      }
    }
  }

  private void saveComment(LjComment ljComment, Map<String, Long> dbIdByThread, LjPost ljPost) {
    PostCommentEntity comment = new PostCommentEntity();
    comment.setContent(ljComment.getText());
    comment.setCreated(ljComment.getDate());
    comment.setParentId(ljComment.getParent() != null ? dbIdByThread.get(ljComment.getParent()) : null);
    comment.setDeleted(false);
    comment.setStoryId(ljPost.getId());
//    comment.setLocalUserId(getOrCreateUser(ljComment).getId());
    // TODO: сохранить пользователя
    comment = doSaveComment(comment);
    dbIdByThread.put(ljComment.getThread(), comment.getId());
  }

  PostCommentEntity doSaveComment(PostCommentEntity postComment) {
//    postComment = storyCommentDao.save(postComment);
//    storyCommentDao.putCache(postComment);
//    storyCommentDao.clearQueryCacheByNamespace("StoryComment:" + postComment.getStoryId());
    return postComment;
  }

  LjPost dumpPostToDisk(LjPost se) throws IOException {
    final DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setAllowReadOnlyProperties(false);
    Yaml yaml = new Yaml(new JodaTimeRepresenter(), dumperOptions);
    final String out = processedDir + File.separatorChar + ljUser.name();
    new File(out).mkdirs();
    yaml.dump(se, new FileWriter(out + File.separatorChar + getPostFileName(se)));
//    se = storyDao.save(se);
//    storyDao.putCache(se);
//    storyDao.clearCache(se.getId());
//    storyDao.clearQueryCache();
    return se;
  }

  private String getPostFileName(LjPost se) {
    return se.getPostId();
  }

  String getOrCreateUser(LjComment ljComment) {
//    LocalUserEntity user = localUserDao.getOneByCriteria(
//        new BaseCriteria().addFilter("username", BaseCriteria.FilterOperator.EQUAL, ljComment.user)
//    );
//    if (user != null) {
//      return user;
//    }
//    user = new LocalUserEntity();
//    user.setUsername(ljComment.user);
//    user = localUserDao.save(user);
//    localUserDao.putCache(user);
//    localUserDao.clearQueryCache();
    return null;
  }

  private void loadCommentByThread(String postId, String threadId, List<LjComment> allComments, int depth) throws IOException {
//    System.out.println("loading comments postId =" + postId + "; threadId =" + threadId + "; allComments count=" + allComments.size() + "; depth =" + depth);
    final String urlToGetThread =
        "http://" + ljUser + ".livejournal.com/" + ljUser
            + "/__rpc_get_thread?journal=" + ljUser + "&itemid="
            + postId + "&thread=" + threadId + "&depth=" + depth;
    final byte[] bytes = urlFetcher.urlToByteArray(urlToGetThread);
    if (bytes == null) {
      return;
    }
    final String commentThread = new String(bytes);
    Gson gson = new Gson();
    final List<LjComment> ljComments = (List<LjComment>) gson.fromJson(
        commentThread, new TypeToken<Collection<LjComment>>() {
    }.getType()
    );
    for (LjComment ljComment : ljComments) {
      if (ljComment.shouldExpand()) {
        loadCommentByThread(postId, ljComment.getThread(), allComments, ljComment.getDepth() - 1);
        // если не сделать этот break, то при нахождении первого коммента, которому нужно сделать
        // expand - все вложенные тоже будут поставленые в очередь и будет выполнена тонна
        // лишних запросов
        break;
      } else if (!allComments.contains(ljComment)) {
        allComments.add(ljComment);
      }
    }
  }

  private void preProcessForSavingAndPrintLjComment(LjComment ljComment, List<LjComment> allCommentsExpanded) throws Exception {
    final Node commentNode = getDocument(ljComment.getHtml());

    int indexIterator = allCommentsExpanded.indexOf(ljComment);
    while (indexIterator >= 0) {
      if (allCommentsExpanded.get(indexIterator).getDepth() < ljComment.getDepth()) {
        ljComment.setParent(allCommentsExpanded.get(indexIterator).getThread());
        break;
      }
      indexIterator--;
    }

    if (commentNode == null) {
      // вероятно сервер вернул что-то не то
//      throw new Exception("Invalid document " + ljComment.html);
      return;
    }
    final XObject comment = XPathAPI.eval(commentNode, ljUser.getXpathCommentText());
    final XObject username = XPathAPI.eval(commentNode, ljUser.getXpathCommentUsername());
    final XObject date = XPathAPI.eval(commentNode, ljUser.getXpathCommentDate());

    String dateString = null;
    if (!ljComment.hasBeenDeleted()) {
      try {
        if (!ljComment.isAccountSuspended()) {
          dateString = stripOuterMostTag(xobjectToString(date));
          dateString = dateString.substring(0, dateString.lastIndexOf(' '));
          DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd hh:mm aa");
          ljComment.setDate(fmt.withZone(DateTimeZone.UTC).parseDateTime(dateString));
          ljComment.setText(ljUser.postProcessCommentText(stripOuterMostTag(xobjectToString(comment))));
        }


        ljComment.setUser(ljComment.isAnonymous() ? "anonymous" : stripOuterMostTag(xobjectToString(username)));
      } catch (StringIndexOutOfBoundsException e) {
        System.out.println("!!!! " + ljComment);
        throw e;
      }
    }
    if (true) {
      return;
    }
    String padding = " ";
    int i = 0;
    while (i < ljComment.getDepth()) {
      System.out.print(padding);
      i++;
    }
    if (!ljComment.hasBeenDeleted()) {
      System.out.println(ljComment.getUser() + " - thread = "
          + ljComment.getThread() + " - parent = " + ljComment.getParent() + " - date = " + dateString
          + " - date parsed " + ljComment.getDate());
    } else {
      System.out.println("posted by unknown deleted user - thread = "
          + ljComment.getThread() + " - parent = " + ljComment.getParent());
    }
    i = 0;
    while (i < ljComment.getDepth()) {
      System.out.print(padding);
      i++;
    }
    if (!ljComment.hasBeenDeleted()) {
      System.out.println(ljComment.getText());
    } else {
      System.out.println("comment has been deleted");
//      System.out.println(urlToGetThread);
    }
  }

  private String persistImages(XObject imagesXObject, String cleanSecondPhase) throws TransformerException, IOException {
    for (int i = 0; i < imagesXObject.nodelist().getLength(); i++) {
      final Node img = imagesXObject.nodelist().item(i);
      final String imgSrc = img.getAttributes().getNamedItem("src").getTextContent();
//      final HTTPResponse response = urlFetchService.fetch(new URL(imgSrc));
      final byte[] imgBytes = urlFetcher.urlToByteArray(imgSrc);
      if (imgBytes == null) {
        continue;
      }
//      final BlobKey blobKey = blobstoreService.saveData(
//          imgSrc.substring(imgSrc.lastIndexOf('/') + 1),
//          "image/jpeg",
//          imgBytes
//      );
//      final String servingUrl = imagesService.getServingUrl(blobKey);
//      cleanSecondPhase = cleanSecondPhase.replaceAll(imgSrc.replaceAll("\\.", "\\.").replaceAll("\\/", "\\/"), servingUrl);
    }
    return cleanSecondPhase;
  }

  private List<String> getTags(XObject tagsXObject) throws TransformerException {
    List<String> result = Lists.newArrayList();
    for (int i = 0; i < tagsXObject.nodelist().getLength(); i++) {
      final Node anchor = tagsXObject.nodelist().item(i);
      result.add(anchor.getTextContent());
    }
    return result;
  }

  /**
   * После использования этого метода может образоваться невалидный XML
   *
   * @param dirtyContent
   * @return
   */
  private String unsafeHtmlClean(String dirtyContent) {
    dirtyContent = dirtyContent.replaceAll("\n", "");
//    dirtyContent = dirtyContent.replaceAll("<\\?xml version=\"1\\.0\" encoding=\"UTF\\-8\"\\?><div class=\"entry\\-content\" xmlns=\"http:\\/\\/www\\.w3\\.org\\/1999\\/xhtml\">", "<p>");
    dirtyContent = dirtyContent.replaceAll("<br clear=\"none\"\\s*/><br clear=\"none\"\\s*/>", "</p><p>");
    dirtyContent = dirtyContent.replaceAll("<br clear=\"none\"\\s*/>", "<br />");
    dirtyContent = dirtyContent.replaceAll("<div class=\"ljtags\">.+</div>", "");
    dirtyContent = dirtyContent.replaceAll("<form action=\"http:\\/\\/www\\.livejournal\\.com\\/update\\.bml\".+<\\/form>", "");
    dirtyContent = dirtyContent.replaceAll("<p>Я на других сервисах.+</p>", "");
    return dirtyContent;
  }

  /**
   * Здесь надо подчистить то, что могло образоваться плохого, после вторичного превращения
   * потенциального неXML в XML
   *
   * @param dirtyContent
   * @return
   */
  private String safeHtmlClean(String dirtyContent) {
//    dirtyContent = dirtyContent.replaceAll("<\\?xml version=\"1\\.0\" encoding=\"UTF\\-8\"\\?><body xmlns=\"http:\\/\\/www\\.w3\\.org\\/1999\\/xhtml\">", "");
//    dirtyContent = dirtyContent.replaceAll("<\\/body>", "");
    dirtyContent = dirtyContent.replaceAll("<p\\/>", "");
    dirtyContent = dirtyContent.replaceAll("<wbr\\/>", "");
    dirtyContent = dirtyContent.replaceAll("<br clear=\"none\"\\s*/>", "<br />");
    return dirtyContent;
  }

  private String xobjectToString(XObject x) throws TransformerException {
    NodeList nodeList = x.nodelist();
    final Node node = nodeList.item(0);
    return nodeToString(node);
  }

  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }

  private String stripOuterMostTag(String text) {
    text = text.substring(text.indexOf('>') + 1);
    text = text.substring(0, text.lastIndexOf('<'));
    return text;
  }

  private Node getDocument(String htmlContent) {
    Node doc = null;
    if (StringUtils.isBlank(htmlContent)) {
      return null;
    }
    try {
      Parser p = new Parser();
      p.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      SAX2DOM sax2dom = new SAX2DOM();
      p.setContentHandler(sax2dom);
      p.parse(new InputSource(new StringReader(htmlContent)));
      doc = sax2dom.getDOM();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXNotSupportedException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXNotRecognizedException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
    return doc;
  }
}
