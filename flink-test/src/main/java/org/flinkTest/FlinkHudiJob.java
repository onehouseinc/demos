package org.flinkTest;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;

public class FlinkHudiJob {
    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // Create the Hudi source table
        tableEnv.executeSql(
                "CREATE TABLE hudi_table ("
                        + " id STRING,"
                        + " `value` STRING,"
                        + " ts TIMESTAMP(3),"
                        + " partition_key STRING"
                        + ")"
                        + "PARTITIONED BY (partition_key)"
                        + "WITH ("
                        + " 'connector' = 'hudi',"
                        + " 'path' = 's3a://acme-lake/acme/acme_default/ce_test_mor_with_partition2',"
                        + " 'table.type' = 'MERGE_ON_READ',"
                        + " 'read.schema.evolution' = 'true',"
                        + " 'read.schema.inference' = 'true'"
                        + ")");

        // Convert Hudi table to DataStream
        DataStream<Row> sourceStream = tableEnv.toDataStream(
                tableEnv.sqlQuery("SELECT * FROM hudi_table"),
                Row.class
        );

        // Process the source stream (example: print the rows)
        sourceStream.print("Source Data");

        // Execute the job
        env.execute("Flink Hudi DataStream Job");
    }
}