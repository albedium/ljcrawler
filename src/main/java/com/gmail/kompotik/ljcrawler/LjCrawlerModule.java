package com.gmail.kompotik.ljcrawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class LjCrawlerModule extends AbstractModule {
  @Override
  protected void configure() {
    Names.bindProperties(binder(), getProperties());
  }

  public Properties getProperties() {
    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream("ljcrawler.properties"));
      return prop;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
