package org.druid.util;


import java.io.IOException;

/**
 * Druid disk space calculator for a datasource segment on historical nodes.
 *
 * This calculator takes number of Druid Rows as one of the input input params.
 * The number of druid rows is the number of rows you expect to have for
 * the segment granularity you defined.
 *
 * Example:
 *
 * lets say you have segment granularity as DAY
 * & query granularity as HOUR
 * & have 3 raw data rows as follows:
 *
 *                   Timestamp col               Gender   age
 * raw data row1:   2017-01-01T23:23:00.Z          male   29
 * raw data row2:   2017-01-01T23:24:00.Z          male   29
 * raw data row3:   2017-01-01T19:24:00.Z          male   29
 *
 * For the above you get TWO druid rows for the Segment of Day 2017-01-01 :
 *
 * druid row 1:   2017-01-01T23:00:00.Z   male   29
 * druid row 2:   2017-01-01T19:00:00.Z   male   29
 *
 * The raw data rows get grouped by (QueryGranularity, dimensions) to give a druid row.
 * Raw data row 1&2 get grouped together as they have same dimension values and same HOUR i.e. 23.
 * Raw data row 3 is by its self as it has a different hour i.e 19, even though its dimensions are same as row1,2
 *
 * The other arguments to this Calc are the number of metric dimensions you have defined and the path to file having column details.
 */
public class Calculator {
  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.out.println("!!! Insufficient arguments for program to run!");
      System.out.println("Usage:");
      System.out.println(
          "Java -cp druid-disk-calc-1.0-SNAPSHOT.jar:. -jar druid-disk-calc-1.0-SNAPSHOT.jar <numDruidRows> <numMetrics> <absolute file path to column details>");
      System.out.println("");
      System.exit(2);
    }

    final long numDruidRows = Long.parseLong(args[0]);
    final int numMetrics = Integer.parseInt(args[1]);
    long totalSize = 0;

    System.out.println("*****************************************");
    System.out.println("Num Druid Rows: " + numDruidRows);
    System.out.println("Num Druid metrics: " + numMetrics);
    System.out.println("Column Details File: " + args[2]);
    System.out.println("*****************************************");
    System.out.println("");


    for (Column column : CsvUtil.readFromCsv(args[2])) {
      System.out.println("==========================================");
      System.out.println(column);
      long dictSize = Formulae.getDictionarySizeForColumn(column.getAvgColumnValueSize(),
          column.getCardinality());
      long colSize = Formulae.getSizeOfColumn(numDruidRows);
      long bitMapIndexSize = Formulae.getBitMapIndexSizeForColumn(column.getCardinality(),
          numDruidRows);
      totalSize = totalSize + dictSize + colSize + bitMapIndexSize;
      System.out.println(
          "Data Dictionary Size + Column Size + Bitmap Size= " + dictSize + " + " + colSize + " + " + bitMapIndexSize);
    }


    System.out.println("");
    System.out.println("Total Size of Dictionary/column/bitmap: " + totalSize);
    long metricSize = (numMetrics * Formulae.getSizeForDefinedMetric(numDruidRows));
    System.out.println("Total Size of " + numMetrics + " metrics: " + metricSize);


    System.out.println("");
    System.out.println("Total bytes: " + (totalSize + metricSize));

  }
}
