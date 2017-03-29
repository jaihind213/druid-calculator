# Druid Calculator
This calculator gives you an estimation of disk space used by a segment of a (http://druid.io/) datasource on the Historical nodes.

 :umbrella: ***Important Note:*** : This calculator determines the size of segment on Historical nodes assuming concise bitmaps are used.

#### Concepts to keep in mind

* Segment Granularity specified at Ingestion time
* Query Granularity specified at Ingestion time
* Data Structures created for a Segment by Druid

#### Segment Data Structures

Based on the druid documentation:

The internal structure of segment files is essentially columnar: the data for each column is laid out in separate data structures:

1. A dictionary that maps Dimension column values (which are always treated as strings) to integer IDs,
2. A list of the column’s values, encoded using the dictionary in point 1, and
3. For each distinct value in the column, a bitmap that indicates which rows contain that value.

#### Calculator Input Arguments 

This calculator needs the following to work:

1. Number of Druid Rows. i.e. number of Druid Rows you expect to have for the segment granularity you defined.

Hence you need to keep in mind the segment and query granularity specified at ingestion time before using this calculator.

Example: lets say you segment granularity as DAY and query granularity as HOUR for the following raw data

raw data row  | TimeStampCol          | gender | age |
--------------| ----------------------| -------|------
raw data row1 | 2017-01-01T23:23:00.Z | male   |  29 |
raw data row2 | 2017-01-01T23:23:00.Z | male   |  29 |
raw data row3 | 2017-01-01T19:23:00.Z | male   |  29 |

For Segment of Day 2017-01-01 , you shall get TWO druid rows which are
    
druid row  | TimeStampCol          | gender | age |
-----------| ----------------------| -------|------
druid row1 | 2017-01-01T23:00:00.Z | male   |  29 |
druid row2 | 2017-01-01T19:00:00.Z | male   |  29 |
     
The raw data rows get grouped by (QueryGranularity, dimensions) to give a druid row.

Raw data row 1&2 get grouped together as they have same dimension values and same HOUR i.e. 23.

Raw data row 3 is by its self as it has a different hour i.e 19, even though its dimensions are same as row1,2 .    
    
2. Number of Druid Metrics defined in the ingestion spec for the datasource.


3. The absolute path to file containing details of the columns specified, which are columnName,ColumnCardinality,AverageColumnValueSizeInBytes

```
    Each line in the file should be of the following format:
    {ColumnName,cardinality,avg column value size in bytes}
    #Example:
    gender,2,1
    passportNum,100000,7
    name,100000,100
```

### How to compile ?

```
mvn clean compile package
```

### How to Run ?

```
cd target
java -cp druid-disk-calc-1.0-SNAPSHOT.jar:. -jar druid-disk-calc-1.0-SNAPSHOT.jar \<number of Druid Rows\> \<number of metric dimensions\> \<absolute path to column details file\>
```

### Formulas

refer to Formulae.java for the logic behind the sizing of each of the above data structures.


### Is it accurate ?

This is an approximation based on my understanding of concise bitmaps and druid docs and attempts
to give u a worst case scenario approximation.

So far from the tests i have run, the numbers are not too far off, but feedback is welcome.  :raised_hands:  :mailbox:  :smile:

Note: Not too sure about the formula i used for metric column size determination  :confused:.


