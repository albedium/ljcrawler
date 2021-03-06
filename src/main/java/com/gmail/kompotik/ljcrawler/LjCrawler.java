package com.gmail.kompotik.ljcrawler;

import org.apache.commons.io.IOUtils;
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
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

@Singleton
public class LjCrawler {
  private final UrlFetcher urlFetcher;
  private String processedDir;
  private String userConfigDir;
  private GroovyObject user;
  public static String FILE_NAME_SUFFIX_POST = "-post.yaml";
  public static String FILE_NAME_SUFFIX_COMMENTS = "-comments.yaml";

  @Inject
  public LjCrawler(
      @Named("ljcrawler.dir.processed") String processedDir,
      @Named("ljcrawler.dir.userConfig") String userConfigDir,
      UrlFetcher urlFetcher) {
    this.processedDir = processedDir;
    this.userConfigDir = userConfigDir;
    this.urlFetcher = urlFetcher;
  }

  public void crawl(String ljUser) throws Exception {
    ClassLoader parent = getClass().getClassLoader();
    GroovyClassLoader loader = new GroovyClassLoader(parent);
    Class groovyClass = loader.parseClass(new File(userConfigDir + File.separator + "LjUser"
        + ljUser.substring(0, 1).toUpperCase()
        + ljUser.substring(1).toLowerCase() + ".groovy")
    );
    user = (GroovyObject) groovyClass.newInstance();

    doCrawl("http://" + user.invokeMethod("getName", null) + ".livejournal.com/");
  }

  private void doCrawl(String urlToSearchArticlesAt) throws Exception {
    final byte[] bytes = urlFetcher.urlToByteArray(urlToSearchArticlesAt, UrlFetchType.Index);
    if (bytes == null) {
      return;
    }

    Node doc = getDocument(new String(bytes));


    XObject titles = XPathAPI.eval(doc, (String) user.invokeMethod("getXpathLinkToStory", null));
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


    XObject prevLink = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathPreviousPageLink", null));
    if (prevLink.nodelist() != null) {
      final Node prevUrl = prevLink.nodelist().item(0).getAttributes().getNamedItem("href");
      System.out.println("============ end of page ===========");
      System.out.println("========= starting new page ========");
      doCrawl(prevUrl.getTextContent());
    }
  }

  private LjPost fetchAndSaveArticle(String storyUrl, LjPost parent) throws Exception {
    final byte[] bytes = urlFetcher.urlToByteArray(storyUrl, UrlFetchType.Post);
    if (bytes == null) {
      return null;
    }
    Node doc = getDocument(new String(bytes));
    LjPost ljPost = new LjPost();
    final XObject h1 = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathH1", null));
    ljPost.setName(h1.nodelist().item(0).getTextContent());
    System.out.println("-- h1 = " + h1.nodelist().item(0).getTextContent());
    String postId = storyUrl.substring(storyUrl.lastIndexOf('/') + 1, storyUrl.lastIndexOf('.'));
    System.out.println("-- id = " + postId);
    ljPost.setPostId(postId);
    final XObject entryContent = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathEntryContent", null));
    final String dirtyContent = stripOuterMostTag(xobjectToString(entryContent));
    String cleanFirstPhase = unsafeHtmlClean(dirtyContent);
    String cleanSecondPhase = safeHtmlClean(stripOuterMostTag(nodeToString(getDocument(cleanFirstPhase).getFirstChild().getFirstChild())));
    if ((Boolean)user.invokeMethod("supportsTags", null)) {
      final XObject tagsXObject = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathTags", null));
      List<String> tags = getTags(tagsXObject);
      System.out.println("-- tags are ");
      System.out.println();
      for (String tag : tags) {
        System.out.println("-  " + tag);
        ljPost.getTags().add(tag);
      }
    }
//    final XObject imagesXObject = XPathAPI.eval(doc, user.invokeMethod("getXpathEntryContent", null) + "//html:img");
//    cleanSecondPhase = persistImages(imagesXObject, cleanSecondPhase);
    ljPost.setContent(
        (String)user.invokeMethod("postProcessEntryText", new Object[]{cleanSecondPhase, ljPost.getName()})
    );
    System.out.println("-- final content = " + ljPost.getContent());
//    ljPost.setStatus(Story.Status.PUBLISHED);
    final XObject date = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathStoryDate", null));
    System.out.println("date = " + date.nodelist().item(0).getTextContent());
    DateTimeFormatter fmt = DateTimeFormat.forPattern((String)user.invokeMethod("getDateFormatForStory", null));
    final DateTime dateTime = fmt.parseDateTime(date.nodelist().item(0).getTextContent());
    /**
     * тут некоторая фигня с датами
     * date = 2011-05-13T15:47:00+03:00
     * parsed date = 2011-05-13T16:47:00.000+04:00
     * formatted date = 2011-05-13 16:47:00
     */
    ljPost.setDate(dateTime.toDate());
    System.out.println("parsed date = " + dateTime);
    System.out.println("formatted date = " + dateTime.toString("YYYY-MM-dd HH:mm:ss"));
    System.out.println();
    System.out.println();
    ljPost.setParentId(parent != null ? parent.getId() : null);

    List<LjComment> allComments = new ArrayList<LjComment>();
    processComments(doc, postId, allComments);
    final XObject totalPages = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathPageCounter", null));
    if (StringUtils.isNotBlank(xobjectToString(totalPages))) {
      final String totalPagesString = stripOuterMostTag(xobjectToString(totalPages));
      final int totalPagesInt = Integer.parseInt(totalPagesString.substring(totalPagesString.lastIndexOf(" ") + 1));
      System.out.println("-- total comment pages found = " + totalPagesInt);
      if (totalPagesInt > 1) {
        for (int i = 2; i <= totalPagesInt; i++) {
          final byte[] bytesComments = urlFetcher.urlToByteArray(
              storyUrl + "?page=" + i,
              UrlFetchType.Comment
          );
          if (bytesComments == null) {
            continue;
          }
          final Node commentsDoc = getDocument(new String(bytesComments));
          processComments(commentsDoc, postId, allComments);
        }
      }
    } else {
      System.out.println("-- no additional comment pages found");
    }

    Map<String, Long> dbIdByThread = Maps.newHashMap();
    for (LjComment ljComment : allComments) {
      preProcessForSavingAndPrintLjComment(ljComment, allComments);
      saveComment(ljComment, dbIdByThread, ljPost);
    }
    ljPost = dumpPostToDisk(ljPost, allComments);

    return ljPost;
  }

  private void processComments(Node doc, String postId, List<LjComment> allComments) throws Exception {
    // загрузим все комменты первого уровня, а потом будет делать по одному запросу на каждую ветку
    // и парсить уже всю ветку
    final XObject commentsTopLevel = XPathAPI.eval(doc, (String)user.invokeMethod("getXpathCommentsWrap", null));
    System.out.println("-- starting to process comments page; top level comment count = " + commentsTopLevel.nodelist().getLength());
    Map<String, Integer> loadByThreadCount = Maps.newHashMap();
    for (int i = 0; i < commentsTopLevel.nodelist().getLength(); i++) {
      final Node commentNode = commentsTopLevel.nodelist().item(i);
      final String threadId = commentNode.getAttributes().getNamedItem("id").getTextContent()
          .substring(LJUser.DIV_COMMENT_IDENTIFICATOR.length()
          );
      loadCommentByThread(postId, threadId, allComments, 0, loadByThreadCount);
   }
  }

  private void saveComment(LjComment ljComment, Map<String, Long> dbIdByThread, LjPost ljPost) {
    PostCommentEntity comment = new PostCommentEntity();
    comment.setContent(ljComment.getText());
//    comment.setCreated(ljComment.getDate());
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

  LjPost dumpPostToDisk(LjPost se, List<LjComment> allComments) throws IOException {
//    final DumperOptions dumperOptions = new DumperOptions();
//    dumperOptions.setAllowReadOnlyProperties(false);
//    dumperOptions.setAllowUnicode(false);
    Yaml yaml = new Yaml();
//    Yaml yaml = new Yaml();
    final String out = processedDir + File.separatorChar + user.invokeMethod("getName", null);
    new File(out).mkdirs();
//    final FileWriter outPost = new FileWriter(out + File.separatorChar + getPostFileName(se));
    final OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(out + File.separatorChar + getPostFileName(se)), "UTF-8");
    yaml.dump(se, wr);
    IOUtils.closeQuietly(wr);

//    final FileWriter outComments = new FileWriter(out + File.separatorChar + getCommentsFileName(se));
    final OutputStreamWriter wr1 = new OutputStreamWriter(new FileOutputStream(out + File.separatorChar + getCommentsFileName(se)), "UTF-8");
    yaml.dumpAll(allComments.iterator(), wr1);
    IOUtils.closeQuietly(wr1);
//    se = storyDao.save(se);
//    storyDao.putCache(se);
//    storyDao.clearCache(se.getId());
//    storyDao.clearQueryCache();
    return se;
  }

  private String getPostFileName(LjPost se) {
    return se.getPostId() + FILE_NAME_SUFFIX_POST;
  }

  private String getCommentsFileName(LjPost se) {
    return se.getPostId() + FILE_NAME_SUFFIX_COMMENTS;
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

  private void loadCommentByThread(String postId, String threadId, List<LjComment> allComments,
                                   int depth, Map<String, Integer> loadByThreadCount) throws IOException {
//    System.out.println("loading comments postId =" + postId + "; threadId =" + threadId + "; allComments count=" + allComments.size() + "; depth =" + depth);
    final String urlToGetThread =
        "http://" + user.invokeMethod("getName", null) + ".livejournal.com/"
            + user.invokeMethod("getName", null)
            + "/__rpc_get_thread?journal=" + user.invokeMethod("getName", null) + "&itemid="
            + postId + "&thread=" + threadId + "&depth=" + depth;
    final byte[] bytes = urlFetcher.urlToByteArray(urlToGetThread, UrlFetchType.Comment);
    if (bytes == null) {
      return;
    }
    final String commentThread = new String(bytes);
    Gson gson = new Gson();
    List<LjComment> ljComments;
    try {
      ljComments = (List<LjComment>) gson.fromJson(
          commentThread, new TypeToken<Collection<LjComment>>() {
      }.getType()
      );
    } catch (Exception e) {
      System.err.println("Error parsing json saved from url " + urlToGetThread
          + " and will be skipping further thread processing " + commentThread);
      e.printStackTrace();
      return;
    }
    for (LjComment ljComment : ljComments) {
      ljComment.setPostId(postId);
      if (!loadByThreadCount.containsKey(ljComment.getThread())) {
        loadByThreadCount.put(ljComment.getThread(), 0);
      }
      loadByThreadCount.put(ljComment.getThread(), loadByThreadCount.get(ljComment.getThread()) + 1);
      // condition introduced to workaround comments, that there are comments (on top level and?)
      // posted by a suspended user - those comments have `expand` button near them,
      // but have no children and this ends up in endless recursion; so we should fail test it
      if (loadByThreadCount.get(ljComment.getThread()) > 5) {
        continue;
      }
      if (ljComment.shouldExpand()) {
        loadCommentByThread(postId, ljComment.getThread(), allComments, ljComment.getDepth() - 1, loadByThreadCount);
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
    final XObject comment = XPathAPI.eval(commentNode, (String)user.invokeMethod("getXpathCommentText", null));
    final XObject username = XPathAPI.eval(commentNode, (String)user.invokeMethod("getXpathCommentUsername", null));
    final XObject date = XPathAPI.eval(commentNode, (String)user.invokeMethod("getXpathCommentDate", null));

    String dateString = null;
    if (!ljComment.hasBeenDeleted()) {
      try {
        if (!ljComment.isAccountSuspended()) {
          dateString = stripOuterMostTag(xobjectToString(date));
          dateString = dateString.substring(0, dateString.lastIndexOf(' '));
          DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd hh:mm aa");
          ljComment.setDate(fmt.withZone(DateTimeZone.UTC).parseDateTime(dateString).toDate());
          ljComment.setText(
              (String)user.invokeMethod("postProcessCommentText", new Object[] {
                  safeHtmlClean(stripOuterMostTag(xobjectToString(comment)))
              })
          );
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
//      System.out.println(ljComment.getUser() + " - thread = "
//          + ljComment.getThread() + " - parent = " + ljComment.getParent() + " - date = " + dateString
//          + " - date parsed " + ljComment.getDate());
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
      // TODO: another UrlFetchType should be introduced if images are to be saved within this project
      final byte[] imgBytes = urlFetcher.urlToByteArray(imgSrc, UrlFetchType.Comment);
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
    // see org.yaml.snakeyaml.reader.StreamReader.checkPrintable
    dirtyContent = dirtyContent.replace("\uFFFD", "");
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
