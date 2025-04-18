package org.flinkTest;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.data.writer.BinaryRowWriter;
import org.apache.flink.table.data.writer.BinaryWriter;
import org.apache.flink.table.runtime.typeutils.InternalSerializers;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.hudi.common.model.HoodieTableType;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.hudi.util.HoodiePipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HudiQuickStart {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: HudiQuickStart <targetTable> <basePath>");
            System.exit(1);
        }

        String targetTable = args[0];
        String basePath = args[1];

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing
        configureCheckpointing(env);

        Map<String, String> options = createHudiOptions(basePath);

        DataStreamSource<RowData> dataStream = env.addSource(new SampleDataSource());
        HoodiePipeline.Builder builder = createHudiPipeline(targetTable, options);

        builder.sink(dataStream, false);
        env.execute("Api_Sink");
    }

    private static void configureCheckpointing(StreamExecutionEnvironment env) {
        env.enableCheckpointing(5000);
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        checkpointConfig.setMinPauseBetweenCheckpoints(10000);
        checkpointConfig.setCheckpointTimeout(60000);
        checkpointConfig.setCheckpointStorage("file:///tmp/hudi_flink_checkpoint_1");
    }

    private static Map<String, String> createHudiOptions(String basePath) {
        Map<String, String> options = new HashMap<>();
        options.put(FlinkOptions.PATH.key(), basePath);
        options.put(FlinkOptions.TABLE_TYPE.key(), HoodieTableType.MERGE_ON_READ.name());
        options.put(FlinkOptions.PRECOMBINE_FIELD.key(), "ts");
        options.put(FlinkOptions.RECORD_KEY_FIELD.key(), "uuid");
        options.put(FlinkOptions.IGNORE_FAILED.key(), "true");
        return options;
    }

    private static HoodiePipeline.Builder createHudiPipeline(String targetTable, Map<String, String> options) {
        return HoodiePipeline.builder(targetTable)
                .column("ts TIMESTAMP(3)")
                .column("uuid VARCHAR(40)")
                .column("rider VARCHAR(20)")
                .column("driver VARCHAR(20)")
                .column("fare DOUBLE")
                .column("city VARCHAR(20)")
                .pk("uuid")
                .partition("city")
                .options(options);
    }

    static class SampleDataSource implements SourceFunction<RowData> {
        private volatile boolean isRunning = true;
        public static BinaryRowData insertRow(Object... fields) {

            DataType ROW_DATA_TYPE = DataTypes.ROW(
                            DataTypes.FIELD("ts", DataTypes.TIMESTAMP(3)), // precombine field
                            DataTypes.FIELD("uuid", DataTypes.VARCHAR(40)),// record key
                            DataTypes.FIELD("rider", DataTypes.VARCHAR(20)),
                            DataTypes.FIELD("driver", DataTypes.VARCHAR(20)),
                            DataTypes.FIELD("fare", DataTypes.DOUBLE()),
                            DataTypes.FIELD("city", DataTypes.VARCHAR(20)))
                    .notNull();
            RowType ROW_TYPE = (RowType) ROW_DATA_TYPE.getLogicalType();
            LogicalType[] types = ROW_TYPE.getFields().stream().map(RowType.RowField::getType)
                    .toArray(LogicalType[]::new);
            BinaryRowData row = new BinaryRowData(fields.length);
            BinaryRowWriter writer = new BinaryRowWriter(row);
            writer.reset();
            for (int i = 0; i < fields.length; i++) {
                Object field = fields[i];
                if (field == null) {
                    writer.setNullAt(i);
                } else {
                    BinaryWriter.write(writer, i, field, types[i], InternalSerializers.create(types[i]));
                }
            }
            writer.complete();
            return row;
        }

        @Override
        public void run(SourceContext<RowData> ctx) throws Exception {
            int batchNum = 0;
            while (isRunning) {
                batchNum ++;
                // For Every Batch, it adds two new rows with RANDOM uuid and updates the row with uuid "334e26e9-8355-45cc-97c6-c31daf0df330"
                List<RowData> DATA_SET_INSERT = Arrays.asList(
                        insertRow(TimestampData.fromEpochMillis(1695159649),
                                StringData.fromString(UUID.randomUUID().toString()), StringData.fromString("rider-A"),
                                StringData.fromString("driver-K"), 19.10, StringData.fromString("san_francisco")),
                        insertRow(TimestampData.fromEpochMillis(1695159649),
                                StringData.fromString(UUID.randomUUID().toString()), StringData.fromString("rider-B"),
                                StringData.fromString("driver-M"), 27.70, StringData.fromString("san_francisco")),
                        insertRow(TimestampData.fromEpochMillis(1695159649),
                                StringData.fromString("334e26e9-8355-45cc-97c6-c31daf0df330"), StringData.fromString("rider-C"),
                                StringData.fromString("driver-L"), 33.90, StringData.fromString("san_francisco"))
                );
                if(batchNum < 11) {
                    // For first 10 batches, inserting 3 records. 2 with random id (INSERTS) and 1 with hardcoded UUID(UPDATE)
                    for (RowData row : DATA_SET_INSERT) {
                        ctx.collect(row);
                    }
                }else{
                    // For 11th Batch, inserting only one record with row kind delete.
                    RowData rowToBeDeleted = DATA_SET_INSERT.get(2);
                    rowToBeDeleted.setRowKind(RowKind.DELETE);
                    ctx.collect(rowToBeDeleted);
                    // Stop the stream once deleted
                    isRunning = false;
                }
                TimeUnit.MILLISECONDS.sleep(10000); // Simulate a delay
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
    }
}
