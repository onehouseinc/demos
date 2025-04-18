package org.flinkTest.FlinkJobGcp;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;

public class FlinkJobGcp {
    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // Create the Hudi source table
        tableEnv.executeSql(
                "CREATE TABLE hudi_table ("
                        + " event STRING,"
                        + " `properties` STRING"
                        + ")"
                        + "WITH ("
                        + " 'connector' = 'hudi',"
                        + " 'path' = 'gs://qa_datalake_bucket/lake_two/sampan_tbd/oh_datagen_events_clickstream/v1',"
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