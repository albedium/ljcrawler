package com.gmail.kompotik.ljcrawler;

import junit.framework.TestCase;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;

public class ListTest extends TestCase {
  public void testSaveLoadLjPost() throws IOException {
    LjPost bean = new LjPost();
    bean.setId(123L);
    bean.getTags().add("1");
    bean.getTags().add("bbb");
    Yaml dumper = new Yaml();
    String doc = dumper.dump(bean);
    System.out.println(doc);
    Yaml loader = new Yaml();
    LjPost parsed = (LjPost) loader.load(doc);
//    assertEquals(etalon, parsed.getDate());
  }
}
