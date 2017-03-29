package org.druid.util;

/**
 * Created by vishnuhr on 28/3/17.
 */
public class Formulae {

  /**
   * get DictionarySize For a Column
   * @param avgSizeOfColumnValue average size of column value. example: if the column is gender, example column value is m,f,na so avg is 1
   * @param columnCardinality example: if the column is gender, possible column values is m,f,na so cardinality is 3
   * @return size of the data dictionary for a column in bytes
   * (avgSizeOfColumnValue + size of integer) x columnCardinality
   */
  public static long getDictionarySizeForColumn(int avgSizeOfColumnValue, long columnCardinality) {
    //say we have column gender with male/female i.e. M/F
    //so for M we assign integer 0
    //so for F we assign integer 1
    //avg col size is 1 byte here.
    //since the dictinary is map<string,int>..
    // the size is 1byte + 4byte integer for M and same for F => 5(avg size+int size) X 2(cardinality) => 10 BYTES
    return (avgSizeOfColumnValue > 0 && columnCardinality > 0) ? ((avgSizeOfColumnValue + Constants.sizeOfInteger) * columnCardinality) : 0;
  }

  /**
   * get Size of Column in bytes.
   * say we have 3 rows with gender column.
   * row 1 has M, row 2 has M, row 3 has F and we have data dictionary as M = integer 0 & F = integer 1
   * The column is visulaized as
   * gender
   *  [M]
   *  [M]
   *  [F]
   *  which is same as
   *  [0]
   *  [0]
   *  [1]
   *  so size = num Rows x size of integer
   * @param numOfDruidRows number of Druid Rows you expect to have for the segment granularity you defined.
      Example:
      lets say you segment granularity as DAY and query granularity as HOUR & have 3 raw data rows as follows
      Timestamp col           Gender   age
      raw data row1:   2017-01-01T23:23:00.Z   male   29
      raw data row2:   2017-01-01T23:24:00.Z   male   29
      raw data row3:   2017-01-01T19:24:00.Z   male   29

      For the above you get TWO druid rows for the Segment of Day 2017-01-01:

      druid row 1:   2017-01-01T23:00:00.Z   male   29
      druid row 2:   2017-01-01T19:00:00.Z   male   29

      The raw data rows get grouped by (QueryGranularity, dimensions) to give a druid row
      Raw data row 1&2 get grouped together as they have same dimension values and same HOUR i.e. 23
      Raw data row 3 is by its self as it has a different hour i.e 19, even though its dimensions are same as row1,2
   * @return size in bytes
   * (numOfDruidRows x size of integer)
   */
  public static long getSizeOfColumn(long numOfDruidRows) {
    return numOfDruidRows > 0 ? (numOfDruidRows * Constants.sizeOfInteger) : 0;
  }

  /**
   * get BitMap Index Size For a Column.
   *
   * Assuming concise bitmap is used for bitmap index. In concise bitmap, 32 bit words are used.
   * In each word first 7 bits(from left) are used for meta data and remaining 25 bits contain either a 1 or 0
   * indicating the presence of that column value in a row.
   * Example: Row1 and row26 has gender male.Then for column value Male,its bitmap index will have bit 1 (from right) of the first & second word set.
   * so for male to be present(at least once) in every consecutive word of bitmap index => for every 2 consecutive words male has
   * to appear at least once in every 25 rows. ie. to appear at least twice in every 50 rows.
   *
   * The consequence of a column value appearing at least twice in 50 consecutive rows, is that in concise bitmap, compression
   * theoretically cannot be applied.
   *
   * so if male is present in row 1,6 and row 26,29
   *                   word 1                                   word 2
   *      |-meta-|  |--25 bits, bit 1,6 are set --|     |-meta-| |-- 25 bits, bit 26,29 are set --|
   * i.e. 1000000   00000 00000 00000 00001  00001     1000000     00000 00000 00000 00000 01001
   *
   * you cannot compress these 2 words because you dont have a nice long consecutive set of zeroes or ones.
   * This is the worst case.
   *
   * best example of worst case i.e. no compression, is for gender column having male/female values
   * and every alternative row is male.
   * you get a series of 1,0  => 101010101010101011010101010101010101010101010 => cant compress
   *
   * So if we have a column Gender
   * and if a column value 'Male' has to occur at least twice in 50 rows.
   * say like row 1 and row 26  are male, means we have 48 zeros which belong to other possible column values.
   * so theoretically we can have 1 + 48 types of Gender i.e. cardinality is 49
   *
   *                       word 1                                   word 2
   *      |-meta-|  |-- 25 bits bit 1 is set  --|     |-meta-| |-- 25 bits bit 1 and bit --|
   * i.e. 1000000   00000 00000 00000 00000 00001     1000000  00000 00000 00000 00000 00001
   *
   * =>  we can say that for compression to not occur in the worst case because of above patterns, the cardinality of the column has to be <= 49
   *
   * i.e. Constants.cardinalityThresholdForColumnValueToBePresentInEveryConsecutiveWordOfBitMapIndex = 49
   *
   * @see org.druid.util.Constants#cardinalityThresholdForColumnValueToBePresentInEveryConsecutiveWordOfBitMapIndex
   *
   * so the formula depends on cardinality !
   *
   * if (columnCardinality >= cardinalityThreshold) {
   *   //Assume sparse distribution of HIGH cardinality colum
   *   //example: say we have PersonID column and each person occurs in 2 out of 1000000 rows => this is sparse example.
   *   //lets say personID 'pid-001' occurs 2 times in 1000000 rows we need 2 bits and if these bits are sparse spread far apart
       // like  |1000000   00000 00000 00000 00001  00000| ...lot of Zeros... then |1000000   00000 00000 00000 00000  00010|
       // so we need need 2 words to at max to store these 2 bits. i.e 2 words => 2 x 4bytes = 8 bytes for 'pid-001' column value
   *   size = getAvgNumberofApperancesOfColumnValue(numOfDruidRows, columnCardinality) * Constants.sizeOfInteger * columnCardinality;
   * }else{
   *   //worst case scenario no compression possible. like the example where male occurs every alternative row.
   *   //we allocate N bits for N rows
   *    size = (numOfDruidRows / Constants.numBitsPerByte) * columnCardinality;
   * }
   *
   *
   * @param columnCardinality example: if the column is gender, possible column values is m,f,na so cardinality is 3
   * @param numOfDruidRows number of Druid Rows you expect to have for the segment granularity you defined.
    Example:
    lets say you segment granularity as DAY and query granularity as HOUR & have 3 raw data rows as follows
    Timestamp col           Gender   age
    raw data row1:   2017-01-01T23:23:00.Z   male   29
    raw data row2:   2017-01-01T23:24:00.Z   male   29
    raw data row3:   2017-01-01T19:24:00.Z   male   29

    For the above you get TWO druid rows for the Segment of Day 2017-01-01:

    druid row 1:   2017-01-01T23:00:00.Z   male   29
    druid row 2:   2017-01-01T19:00:00.Z   male   29

    The raw data rows get grouped by (QueryGranularity, dimensions) to give a druid row
    Raw data row 1&2 get grouped together as they have same dimension values and same HOUR i.e. 23
    Raw data row 3 is by its self as it has a different hour i.e 19, even though its dimensions are same as row1,2
   * @return long size of bitmap index in bytes
   */
  public static long getBitMapIndexSizeForColumn(long columnCardinality, long numOfDruidRows) {
    long size = 0;
    if (columnCardinality > 0 && numOfDruidRows > 0) {
      if (columnCardinality > Constants.cardinalityThresholdForColumnValueToBePresentInEveryConsecutiveWordOfBitMapIndex) {
        //lets say column value Male occurs 2 times in 1000 rows we need 2 bits and if these bits are sparse spread far apart
        // like  |1000000   00000 00000 00000 00001  00010| ...lot of Zeros... then |1000000   00000 00000 00000 00001  00010|
        // so we need need 2 words to at max to store these 2 bits. i.e 2 words => 2 x 4bytes = 8 bytes for Male column value
        size = getAvgNumberofApperancesOfColumnValue(numOfDruidRows,
            columnCardinality) * Constants.sizeOfInteger * columnCardinality;
      } else {
        //worst case no compression. so we need n bits for numOfDruidRows
        //we allocate N bits for N rows like the example where male occurs every alternative row.
        size = (numOfDruidRows / Constants.numBitsPerByte) * columnCardinality;
      }
    }
    return size;
  }


  public static long getSizeForDefinedMetric(long numOfDruidRows) {
    //not yet verified this logic
    return numOfDruidRows > 0 ? numOfDruidRows * Constants.sizeOfLong : 0;
  }

  private static long getAvgNumberofApperancesOfColumnValue(long numOfDruidRows,
      long columnCardinality) {
    //lets do uniform distribution
    return (numOfDruidRows > 0 && columnCardinality > 0) ? (numOfDruidRows / columnCardinality) : 0;
  }

}
