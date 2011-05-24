package com.gmail.kompotik.ljcrawler;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class Importer {
  private final String ljUserName;
  private final String processedDir;

  /**
   * @param ljUserName LiveJournal name of the user. E. g. for http://tema.livejournal.com
   * this parameter should be set to `tema`.
   * @param processedDir Directory containing dumped data
   */
  @Inject
  public Importer(String ljUserName, @Named("ljcrawler.dir.processed") String processedDir) {
    this.ljUserName = ljUserName;
    this.processedDir = processedDir;
  }

  public File[] getPostFiles() {
    return new File(processedDir + File.separatorChar + ljUserName.toLowerCase()).listFiles(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith(LjCrawler.FILE_NAME_SUFFIX_POST);
          }
        }
    );
  }

  public File[] getCommentFiles() {
    return new File(processedDir + File.separatorChar + ljUserName.toLowerCase()).listFiles(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith(LjCrawler.FILE_NAME_SUFFIX_COMMENTS);
          }
        }
    );
  }

  public LjPost fileToPost(File file) throws Exception {
    Yaml yaml = new Yaml();
    try {
      final FileInputStream io = new FileInputStream(file);
      final LjPost result = (LjPost) yaml.load(io);
      IOUtils.closeQuietly(io);
      return result;
    } catch (Exception e) {
      System.out.println("error with file " + file);
      throw e;
    }
  }

  public List<LjComment> fileToComment(File file) throws Exception {
    Yaml yaml = new Yaml();
    try {
      final FileInputStream io = new FileInputStream(file);
      final Iterable<Object> objectIterable = yaml.loadAll(io);
      List<LjComment> result = Lists.newArrayList();
      while (objectIterable.iterator().hasNext()) {
        result.add((LjComment) objectIterable.iterator().next());
      }
      IOUtils.closeQuietly(io);
      return result;
    } catch (Exception e) {
      System.out.println("error with file " + file);
      throw e;
    }
  }
}
