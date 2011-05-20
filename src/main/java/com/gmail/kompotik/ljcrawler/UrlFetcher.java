package com.gmail.kompotik.ljcrawler;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;

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
  private final HttpClient httpclient;
  private static final int MAX_ATTEMPT = 5;

  @Inject
  public UrlFetcher(@Named("ljcrawler.dir.webcache") String tmpDir) {
    this.TMP_FOLDER = tmpDir;

    httpclient = new DefaultHttpClient();
    // according to http://www.livejournal.com/bots/ it is advised to set proper user agent
    HttpProtocolParams.setUserAgent(
        httpclient.getParams(),
        "https://github.com/kompot/ljcrawler; kompotik@gmail.com"
    );
  }

  public byte[] urlToByteArray(String url) throws IOException {
    return urlToByteArray(url, 0);
  }

  private byte[] urlToByteArray(String url, int attempt) throws IOException {
    if (attempt > MAX_ATTEMPT) {
      return null;
    }
    // make sure that URL has not already been cached on disk
    Base64 encoder = new Base64(0, null, true);
    final String urlBase64representation = encoder.encodeToString(url.getBytes());

    final String fileName = TMP_FOLDER + File.separatorChar + urlBase64representation;
    File file = new File(fileName);
    if (file.exists()) {
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
      return urlToByteArray(url, attempt);
    }

    final byte[] responseContent = IOUtils.toByteArray(response.getEntity().getContent());
    final File file1 = new File(fileName);
    file1.getParentFile().mkdirs();
    final FileOutputStream output = new FileOutputStream(file1);
    IOUtils.write(responseContent, output);
    IOUtils.closeQuietly(output);
    return responseContent;
  }
}
