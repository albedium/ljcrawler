package com.gmail.kompotik.ljcrawler;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EntryPoint {
  public static void main(String[] args) throws Exception {
    final Injector injector = Guice.createInjector(new LjCrawlerModule());
    LjCrawler crawler = injector.getInstance(LjCrawler.class);
//    crawler.crawl(LjUser.zyalt);
    crawler.crawl(LjUser.drugoi);
    System.exit(0);
  }
}
