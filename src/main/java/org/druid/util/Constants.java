package org.druid.util;

/**
 * Created by vishnuhr on 28/3/17.
 */
public class Constants {

  public static final int sizeOfInteger = 4; //bytes
  public static final int sizeOfLong = 8; //bytes
  public static final int numBitsPerByte = 8;
  public static final int numBitsPerWord = numBitsPerByte * 4;

  /**
   * Assuming concise bitmap is used for bitmap index. In concise bitmap, 32 bit words are used.
   * In each word first 7 bits(from left) are used for meta data and remaining 25 bits contain either a 1 or 0
   * indicating the presence of that column value in a row.
   * Example: Row1 and row26 has gender male. Then for column value Male,its bitmap index will have bit 1 (from right) of the first & second word set.
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
   * best example of worst case i.e. no compression , is for gender column having male/female values
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
   * =>  we can say that for compression to not occur in the worst case because of above patterns
   * the cardinality of the column has to be <= 49
   *
   *
   */
  public static final int cardinalityThresholdForColumnValueToBePresentInEveryConsecutiveWordOfBitMapIndex = 49;

}
