package org.druid.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by vishnuhr on 28/3/17.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Column {

  private String name;
  private long cardinality;
  private int avgColumnValueSize; //in bytes

}
