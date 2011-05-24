package com.gmail.kompotik.ljcrawler;

import java.io.File;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class OtherEntryPoint {
  public static void main(String[] args) throws Exception {
    final Injector injector = Guice.createInjector(new LjCrawlerModule());
    Importer importer = new Importer(args[0], "processed");
    for (File file : importer.getPostFiles()) {
      System.out.println("!!!! " + importer.fileToPost(file).getName());
    }
    for (File file : importer.getCommentFiles()) {
      final List<LjComment> ljComments = importer.fileToComment(file);
      System.out.println("!!!! " + ljComments.get(0).getPostId() + " - " + ljComments.size());
    }
  }
}
