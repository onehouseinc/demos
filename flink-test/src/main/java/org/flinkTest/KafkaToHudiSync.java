package org.flinkTest;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.data.writer.BinaryRowWriter;
import org.apache.flink.table.data.writer.BinaryWriter;
import org.apache.flink.table.runtime.typeutils.InternalSerializers;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.hudi.client.clustering.plan.strategy.FlinkConsistentBucketClusteringPlanStrategy;
import org.apache.hudi.common.model.HoodieTableType;
import org.apache.hudi.common.model.WriteOperationType;
import org.apache.hudi.config.HoodieClusteringConfig;
import org.apache.hudi.config.HoodieIndexConfig;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.index.HoodieIndex;
import org.apache.hudi.util.HoodiePipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaToHudiSync {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new Exception("Please pass 4 parameters - PipelineName HudiBasePath KafkGroupId kafkaTopicName");
        }

        // Parse command line arguments
        String pipelineName = args[0];
        String hudiBasePath = args[1];
        String kafkaGroupId = args[2];
        String kafkaTopicName = args[3];

        // Create a Flink execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        configureCheckpointing(env);

        // Set up Kafka source
//        FlinkKafkaConsumer<GenericRecord> kafkaConsumer = createKafkaConsumer(kafkaTopicName, kafkaGroupId,kafkaTopicName + "-value");
        KafkaSource<GenericRecord> kafkaSource = createKafkaSource(kafkaTopicName, kafkaGroupId,kafkaTopicName + "-value");

        // Create a Kafka stream
//        DataStream<GenericRecord> kafkaStream = env.addSource(kafkaConsumer);

        DataStream<GenericRecord> kafkaStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source"
        );

        // Transform Kafka data to Hudi records
        DataStream<RowData> transformedStream = kafkaStream
                .map(new HudiDataSource());

        // Define Hudi target table and options
        String targetTable = "hudi_table";
        Map<String, String> options = createHudiOptions(hudiBasePath);

        // Define HoodiePipeline.Builder for configuring the Hudi write
        HoodiePipeline.Builder builder = createHudiPipeline(targetTable, options);

        // Write to Hudi
        builder.sink(transformedStream, false);

        // Execute the Flink job
        env.execute(pipelineName);
    }

    // Configure Flink checkpointing settings
    private static void configureCheckpointing(StreamExecutionEnvironment env) {
        env.enableCheckpointing(60000); // Checkpoint every 50 seconds
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        checkpointConfig.setMinPauseBetweenCheckpoints(5000); // Minimum time between checkpoints
        checkpointConfig.setCheckpointTimeout(60000); // Checkpoint timeout in milliseconds
        checkpointConfig.setCheckpointStorage("file:///tmp/hudi_flink_checkpoint_2");
        checkpointConfig.setMaxConcurrentCheckpoints(1);
    }

    // Create a Kafka consumer with specified properties
    private static FlinkKafkaConsumer<GenericRecord> createKafkaConsumer(String topicName, String kafkaGroupId, String schemaName) {
        String schemaRegistryUrl = "https://psrc-1wydj.us-east-2.aws.confluent.cloud";
        String schemaRegistryApiKey = "CGNTRPZW2PBZO3OH";
        String schemaRegistryApiSecret = "ju+ZAt7GebM7sYH/k9Z1YA4xAa8Ztn33B59oMYzYiEmaXbguEE2IQg0VkPQudmV0";
        String kafkaSaslUsername = "PJPJ54KESMFJOVTE";
        String kafkaSaslPassword = "KAepEh3eKrMctBd440J4z5cUhkDwij5C3gaCWTxZlMOm+h2dYY4yLsgIR5alHLqm";

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "pkc-ymrq7.us-east-2.aws.confluent.cloud:9092");
        properties.setProperty("group.id", kafkaGroupId);
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("sasl.jaas.config", String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                kafkaSaslUsername, kafkaSaslPassword));

        // Set a small fetch max bytes to further control batching
        properties.setProperty("max.partition.fetch.bytes", "1048576"); // 1MB

        // Schema Registry configuration
        Map<String, String> registryConfig = new HashMap<>();
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG,
                schemaRegistryApiKey + ":" + schemaRegistryApiSecret);

        // Create generic record deserializer
        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"KafkaEvent\",\"fields\":[{\"name\":\"event\",\"type\":\"string\"},{\"name\":\"properties\",\"type\":\"string\"}]}");
        ConfluentRegistryAvroDeserializationSchema<GenericRecord> deserializationSchema =
                ConfluentRegistryAvroDeserializationSchema.forGeneric(schema, schemaRegistryUrl, registryConfig);

        return new FlinkKafkaConsumer<>(
                topicName,
                deserializationSchema,
                properties);
    }

    private static KafkaSource<GenericRecord> createKafkaSource(String topicName, String kafkaGroupId, String schemaName) {
        String schemaRegistryUrl = "https://psrc-1wydj.us-east-2.aws.confluent.cloud";
        String schemaRegistryApiKey = "CGNTRPZW2PBZO3OH";
        String schemaRegistryApiSecret = "ju+ZAt7GebM7sYH/k9Z1YA4xAa8Ztn33B59oMYzYiEmaXbguEE2IQg0VkPQudmV0";
        String kafkaSaslUsername = "PJPJ54KESMFJOVTE";
        String kafkaSaslPassword = "KAepEh3eKrMctBd440J4z5cUhkDwij5C3gaCWTxZlMOm+h2dYY4yLsgIR5alHLqm";

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "pkc-ymrq7.us-east-2.aws.confluent.cloud:9092");
        properties.setProperty("group.id", kafkaGroupId);
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("sasl.jaas.config", String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                kafkaSaslUsername, kafkaSaslPassword));

        // Set a small fetch max bytes to further control batching
        properties.setProperty("max.partition.fetch.bytes", "1048576"); // 1MB

        // Schema Registry configuration
        Map<String, String> registryConfig = new HashMap<>();
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        registryConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG,
                schemaRegistryApiKey + ":" + schemaRegistryApiSecret);

        // Create generic record deserializer
        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"KafkaEvent\",\"fields\":[{\"name\":\"event\",\"type\":\"string\"},{\"name\":\"properties\",\"type\":\"string\"}]}");
        ConfluentRegistryAvroDeserializationSchema<GenericRecord> deserializationSchema =
                ConfluentRegistryAvroDeserializationSchema.forGeneric(schema, schemaRegistryUrl, registryConfig);

        return KafkaSource.<GenericRecord>builder()
                .setBootstrapServers("pkc-ymrq7.us-east-2.aws.confluent.cloud:9092")
                .setTopics(topicName)
                .setGroupId(kafkaGroupId)
                .setProperties(properties)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(deserializationSchema).build();
    }

    // Create Hudi options for the data sink
    private static Map<String, String> createHudiOptions(String basePath) {
        Map<String, String> options = new HashMap<>();
        options.put(FlinkOptions.PATH.key(), basePath);
        options.put(FlinkOptions.TABLE_TYPE.key(), HoodieTableType.MERGE_ON_READ.name());
        options.put(FlinkOptions.PRECOMBINE_FIELD.key(), "ts");
        options.put(FlinkOptions.IGNORE_FAILED.key(), "true");
        options.put(FlinkOptions.WRITE_PARQUET_MAX_FILE_SIZE.key(), "-1");
        options.put(HoodieIndexConfig.BUCKET_INDEX_MIN_NUM_BUCKETS.key(), "1");
        options.put(HoodieIndexConfig.BUCKET_INDEX_MAX_NUM_BUCKETS.key(), "8");
        options.put(HoodieIndexConfig.BUCKET_SPLIT_THRESHOLD.key(), String.valueOf(1 / 1024.0 / 1024.0));
        options.put(FlinkOptions.BUCKET_INDEX_NUM_BUCKETS.key(), "1");
        options.put(FlinkOptions.INDEX_TYPE.key(), HoodieIndex.IndexType.BUCKET.name());
        options.put(FlinkOptions.OPERATION.key(), WriteOperationType.UPSERT.name());
        options.put(FlinkOptions.CLUSTERING_SCHEDULE_ENABLED.key(), "true");
        options.put(FlinkOptions.BUCKET_INDEX_ENGINE_TYPE.key(), HoodieIndex.BucketIndexEngineType.CONSISTENT_HASHING.name());
        options.put(FlinkOptions.CLUSTERING_PLAN_STRATEGY_CLASS.key(), FlinkConsistentBucketClusteringPlanStrategy.class.getName());
        options.put(HoodieClusteringConfig.EXECUTION_STRATEGY_CLASS_NAME.key(), "org.apache.hudi.client.clustering.run.strategy.SparkConsistentBucketClusteringExecutionStrategy");

        options.put(FlinkOptions.COMPACTION_TRIGGER_STRATEGY.key(), "num_commits");
        options.put(FlinkOptions.COMPACTION_DELTA_COMMITS.key(), "1");
        options.put(FlinkOptions.COMPACTION_DELTA_SECONDS.key(), "0");
        options.put(FlinkOptions.COMPACTION_MAX_MEMORY.key(), "100");

        // Control the size of the write batch
        options.put(FlinkOptions.WRITE_BATCH_SIZE.key(), "20");
        options.put(FlinkOptions.WRITE_TASK_MAX_SIZE.key(), "1024");

        // Ensure small file handling to generate more commits
        options.put(FlinkOptions.WRITE_LOG_BLOCK_SIZE.key(), "1");
        options.put(FlinkOptions.WRITE_PARQUET_BLOCK_SIZE.key(), "1");
        return options;
    }

    // Create a HoodiePipeline.Builder with specified target table and options
    private static HoodiePipeline.Builder createHudiPipeline(String targetTable, Map<String, String> options) {
        return HoodiePipeline.builder(targetTable)
                .column("uuid VARCHAR(256)")
                .column("name VARCHAR(10)")
                .column("age INT")
                .column("ts TIMESTAMP(3)")
                .column("`partition` VARCHAR(20)")
                .pk("uuid")
                .partition("partition")
                .options(options);
    }

    // Define the schema for Hudi records
    public static final DataType ROW_DATA_TYPE = DataTypes.ROW(
                    DataTypes.FIELD("uuid", DataTypes.VARCHAR(256)), // record key
                    DataTypes.FIELD("name", DataTypes.VARCHAR(10)),
                    DataTypes.FIELD("age", DataTypes.INT()),
                    DataTypes.FIELD("ts", DataTypes.TIMESTAMP(3)), // precombine field
                    DataTypes.FIELD("partition", DataTypes.VARCHAR(10)))
            .notNull();

    // Create a Hudi record from specified fields
    public static BinaryRowData insertRow(RowType rowType, Object... fields) {
        LogicalType[] types = rowType.getFields().stream().map(RowType.RowField::getType)
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

    // Overloaded method for creating a Hudi record using the default schema
    public static BinaryRowData insertRow(Object... fields) {
        return insertRow((RowType) ROW_DATA_TYPE.getLogicalType(), fields);
    }

    // Mapper to convert Kafka data to Hudi records
    static class HudiDataSource implements MapFunction<GenericRecord, RowData> {
        @Override
        public RowData map(GenericRecord record) throws Exception {
            // Extract fields from the GenericRecord
            String event = record.get("event") != null ? record.get("event").toString() : "";
            String properties = record.get("properties") != null ? record.get("properties").toString() : "";

            // Get current timestamp
            long timestamp = System.currentTimeMillis();

            // Assuming your insertRow method expects these parameters
            return insertRow(
                    StringData.fromString(event),
                    StringData.fromString(properties),
                    23,
                    TimestampData.fromEpochMillis(timestamp),
                    StringData.fromString("par1")
            );
        }
    }
}
