package com.gmail.kompotik.ljcrawler;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class UrlFetcher {
  private final String TMP_FOLDER;
  private int ttlIndexPage;
  private int ttlPostPage;
  private int ttlCommentThread;
  private final HttpClient httpclient;
  private static final int MAX_ATTEMPT = 5;
  private static final int MILLISECONDS_A_DAY = 24 * 60 * 60 * 1000;

  @Inject
  public UrlFetcher(
      @Named("ljcrawler.dir.webcache") String tmpDir,
      @Named("ljcrawler.ttl.indexPage") int ttlIndexPage,
      @Named("ljcrawler.ttl.postPage") int ttlPostPage,
      @Named("ljcrawler.ttl.commentThread") int ttlCommentThread
  ) {
    this.TMP_FOLDER = tmpDir;
    this.ttlIndexPage = ttlIndexPage;
    this.ttlPostPage = ttlPostPage;
    this.ttlCommentThread = ttlCommentThread;

    httpclient = new DefaultHttpClient();
    // according to http://www.livejournal.com/bots/ it is advised to set proper user agent
    HttpProtocolParams.setUserAgent(
        httpclient.getParams(),
        "https://github.com/kompot/ljcrawler; kompotik@gmail.com"
    );
  }

  public byte[] urlToByteArray(String url, UrlFetchType fetchType) throws IOException {
    return urlToByteArray(url, 0, fetchType);
  }

  private byte[] urlToByteArray(String url, int attempt, UrlFetchType fetchType) throws IOException {
    if (attempt > MAX_ATTEMPT) {
      return null;
    }
    // make sure that URL has not already been cached on disk
    final String urlBase64representation = Base64.encodeBase64URLSafeString(DigestUtils.sha512(url));

    final String fileName = TMP_FOLDER + File.separatorChar + urlBase64representation;
    File file = new File(fileName);

    if (isValidCacheOnDisk(file, fetchType)) {
      final FileInputStream content = new FileInputStream(file);
      final byte[] bytes = IOUtils.toByteArray(content);
      IOUtils.closeQuietly(content);
      return bytes;
    }

    final HttpResponse response;
    try {
      Thread.sleep(200);
      HttpGet httpget = new HttpGet(url);
      response = httpclient.execute(httpget);
    } catch (Exception e) {
      e.printStackTrace();
      attempt++;
      System.out.println("Will be trying to fetch " + url + " in " + attempt + " seconds");
      try {
        Thread.sleep(1000 * attempt);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      return urlToByteArray(url, attempt, fetchType);
    }

    final byte[] responseContent = IOUtils.toByteArray(response.getEntity().getContent());
    final File file1 = new File(fileName);
    file1.getParentFile().mkdirs();
    final FileOutputStream output = new FileOutputStream(file1);
    IOUtils.write(responseContent, output);
    IOUtils.closeQuietly(output);
    return responseContent;
  }

  private boolean isValidCacheOnDisk(File file, UrlFetchType fetchType) {
    if (!file.exists()) {
      return false;
    }
    if (FileUtils.isFileOlder(file,
        new DateTime().minusMillis(MILLISECONDS_A_DAY * getDaysByFetchType(fetchType)).toDate()
    )) {
      return false;
    }
    // TODO: check that if fetchType == UrlFetchType.Comment then the result on disk is valid JSON
    // TODO: probably check that if fetchType == UrlFetchType.Post|Index then the result on disk is
    // valid HTML
    // may be these two checks should be also performed when fetching content by HTTPClient
    return true;
  }

  private int getDaysByFetchType(UrlFetchType fetchType) {
    if (fetchType == UrlFetchType.Index) {
      return ttlIndexPage;
    }
    if (fetchType == UrlFetchType.Post) {
      return ttlPostPage;
    }
    if (fetchType == UrlFetchType.Comment) {
      return ttlCommentThread;
    }
    // by default cache is stored for one day
    return 1;
  }
}
