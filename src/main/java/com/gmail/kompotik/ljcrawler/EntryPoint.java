package com.gmail.kompotik.ljcrawler;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EntryPoint {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("You must specify users to be crawled");
      System.exit(1);
    }
    final Injector injector = Guice.createInjector(new LjCrawlerModule());
    LjCrawler crawler = injector.getInstance(LjCrawler.class);
    for (String arg : args) {
      crawler.crawl(arg);
    }
    System.exit(0);
  }
}
