package org.druid.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishnuhr on 28/3/17.
 */
public class CsvUtil {

  public static List<Column> readFromCsv(String csvFile) throws IOException {
    List<Column> result = new ArrayList<Column>();
    String line = "";
    String cvsSplitBy = ",";
    FileReader fr = new FileReader(csvFile);
    BufferedReader br = new BufferedReader(fr);

    try {
      while ((line = br.readLine()) != null) {
        // use comma as separator
        if(line.startsWith("#")){
          continue;
        }
        String[] columnDetails = line.split(cvsSplitBy);
        Column column = new Column();
        column.setName(columnDetails[0]);
        column.setCardinality(Long.parseLong(columnDetails[1]));
        column.setAvgColumnValueSize(Integer.parseInt(columnDetails[2]));
        result.add(column);
      }
    } finally {
      if(br!=null){
        br.close();
      }
      if(fr!=null){
        fr.close();
      }
    }
    return result;
  }

}
