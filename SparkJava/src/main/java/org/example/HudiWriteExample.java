package org.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;
import org.apache.hudi.DataSourceWriteOptions;

import java.util.Arrays;
import java.util.List;

public class HudiWriteExample {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("org.example.HudiWriteExample")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//                .enableHiveSupport() // Enable Hive for saveAsTable
                .getOrCreate();

        // Define schema
        StructType schema = new StructType(new StructField[]{
                new StructField("id", DataTypes.LongType, false, Metadata.empty()),
                new StructField("name", DataTypes.StringType, true, Metadata.empty()),
                new StructField("age", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("company", DataTypes.StringType, true, Metadata.empty()),
                new StructField("join_date", DataTypes.StringType, true, Metadata.empty()),
                new StructField("location", DataTypes.StringType, true, Metadata.empty())
        });

        // Define data
        List<Row> data = Arrays.asList(
                RowFactory.create(1L, "Richard Hendricks", 30, "Pied Piper", "2014-05-15", "Mountain View"),
                RowFactory.create(2L, "Erlich Bachman", 35, "Aviato", "2013-08-22", "San Francisco"),
                RowFactory.create(3L, "Jared Dunn", 35, "Pied Piper", "2015-03-10", "Palo Alto"),
                RowFactory.create(4L, "Dinesh Chugtai", 28, "Pied Piper", "2016-01-20", "San Jose"),
                RowFactory.create(5L, "Bertram Gilfoyle", 32, "Pied Piper", "2015-11-05", "San Francisco")
        );

        // Create DataFrame
        Dataset<Row> df = spark.createDataFrame(data, schema);

        // Hudi table options
        String tableType = DataSourceWriteOptions.MOR_TABLE_TYPE_OPT_VAL(); // MOR (Merge-On-Read)
        String databaseName = "spark_demo";
        String tableName = "employees";
        String tablePath = "s3://lakehouse-albert-us-west-2/demolake/spark_demo/employees";

        // Write to Hudi table
        df.write()
                .format("hudi")
                .option(DataSourceWriteOptions.RECORDKEY_FIELD().key(), "id")
                .option(DataSourceWriteOptions.PRECOMBINE_FIELD().key(), "join_date")
                .option(DataSourceWriteOptions.TABLE_TYPE().key(), tableType)
                .option("hoodie.table.name", tableName)
                .option("path", tablePath)
                .mode("append")
                .saveAsTable(String.format("%s.%s", databaseName, tableName)); // Save and register table in Hive metastore
    }
}