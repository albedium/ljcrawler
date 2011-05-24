package com.gmail.kompotik.ljcrawler;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;

public class JodaTimeTest extends TestCase {
  private static final long timestamp = 1000000000000L;

//  public void testDump() throws IOException {
//    DateTime time = new DateTime(timestamp, DateTimeZone.UTC);
//    Yaml yaml = new Yaml(new JodaTimeRepresenter());
//    String joda = yaml.dump(time);
//    String date = new Yaml().dump(new Date(timestamp));
//    assertEquals(date, joda);
//  }
//
//  public void testLoad() throws IOException {
//    Yaml yaml = new Yaml(new JodaTimeConstructor());
//    DateTime time = (DateTime) yaml.load("2001-09-09T01:46:40Z");
//    assertEquals(new DateTime(timestamp, DateTimeZone.UTC), time);
//  }

  public void testLoadBean1() throws IOException {
    LjPost bean = new LjPost();
    bean.setId(123L);
    DateTime etalon = new DateTime(timestamp, DateTimeZone.UTC);
    bean.setDate(etalon.toDate());
    bean.getTags().add("первый тег");
    bean.getTags().add("еще один тег");
//    JavaBeanDumper javaBeanDumper = new JavaBeanDumper(new JodaTimeRepresenter(), new DumperOptions());
    Yaml dumper = new Yaml();
    String doc = dumper.dump(bean);
    System.out.println(doc);
    Yaml loader = new Yaml();
    LjPost parsed = (LjPost) loader.load(doc);
    assertEquals(etalon.toDate().getTime(), parsed.getDate().getTime());
  }
}
