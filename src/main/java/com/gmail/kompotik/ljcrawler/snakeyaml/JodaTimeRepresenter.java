package com.gmail.kompotik.ljcrawler.snakeyaml;

import org.joda.time.DateTime;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Date;

import com.gmail.kompotik.ljcrawler.LjComment;

public class JodaTimeRepresenter extends Representer {
  public JodaTimeRepresenter() {
    multiRepresenters.put(DateTime.class, new RepresentJodaDateTime());
//    multiRepresenters.put(LjComment.class, new RepresentLjComment());
  }

  private class RepresentJodaDateTime extends RepresentDate {
    public Node representData(Object data) {
      DateTime date = (DateTime) data;
      return super.representData(new Date(date.getMillis()));
    }
  }

//  private class RepresentLjComment extends Representer {
//    @Override
//    public Node representData(Object data) {
//
//    }
//  }

}