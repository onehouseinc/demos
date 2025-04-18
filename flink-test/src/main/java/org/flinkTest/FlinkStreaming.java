//package org.flinkTest;
//
//import org.apache.avro.Schema;
//import org.apache.avro.generic.GenericRecord;
//import org.apache.flink.api.common.eventtime.WatermarkStrategy;
//import org.apache.flink.connector.kafka.source.KafkaSource;
//import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
//import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
//import org.apache.flink.table.catalog.hive.HiveCatalog;
//import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
//import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
//import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
//
//import java.util.Arrays;
//import java.util.Base64;
//import java.util.List;
//import java.util.Properties;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.stream.Collectors;
//
//public class FlinkStreaming {
//    public static void main(String[] args) throws Exception {
//        // Initialize Flink environments
//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
//
//        // Hive catalog configuration
////        String hiveCatalogName = "hive_catalog";
////        String defaultDatabase = "default";
////        String hiveConfDir = "/opt/hive/conf";
////
////        HiveCatalog hiveCatalog = new HiveCatalog(hiveCatalogName, defaultDatabase, hiveConfDir);
////        tableEnv.registerCatalog(hiveCatalogName, hiveCatalog);
////        tableEnv.useCatalog(hiveCatalogName);
//
//        // Kafka and Schema Registry configuration
//        String bootstrapServers = "pkc-ymrq7.us-east-2.aws.confluent.cloud:9092";
//        String topic = "oh-datagen-events-clickstream";
//        String schemaRegistryUrl = "https://psrc-1wydj.us-east-2.aws.confluent.cloud";
//        String schemaRegistryApiKey = "CGNTRPZW2PBZO3OH";
//        String schemaRegistryApiSecret = "ju+ZAt7GebM7sYH/k9Z1YA4xAa8Ztn33B59oMYzYiEmaXbguEE2IQg0VkPQudmV0";
//        String kafkaSaslUsername = "PJPJ54KESMFJOVTE";
//        String kafkaSaslPassword = "KAepEh3eKrMctBd440J4z5cUhkDwij5C3gaCWTxZlMOm+h2dYY4yLsgIR5alHLqm";
////
////        // Set up Schema Registry client
////        SchemaRegistryClient schemaRegistryClient = getSchemaRegistryClient(
////                schemaRegistryUrl,
////                schemaRegistryApiKey,
////                schemaRegistryApiSecret
////        );
////
////        // Get latest schema
////        String subject = topic + "-value";
////        Schema avroSchema = new Schema.Parser().parse(
////                schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema()
////        );
////
//        // Configure Schema Registry properties for deserializer
//        Map<String, String> schemaRegistryConfigs = new HashMap<>();
//        schemaRegistryConfigs.put("basic.auth.credentials.source", "USER_INFO");
//        schemaRegistryConfigs.put("basic.auth.user.info",
//                schemaRegistryApiKey + ":" + schemaRegistryApiSecret);
//        schemaRegistryConfigs.put("schema.registry.url", schemaRegistryUrl);
//
//        // Configure Kafka properties
//        Properties kafkaProps = new Properties();
//        kafkaProps.setProperty("security.protocol", "SASL_SSL");
//        kafkaProps.setProperty("sasl.mechanism", "PLAIN");
//        kafkaProps.setProperty("sasl.jaas.config",
//                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
//                        kafkaSaslUsername, kafkaSaslPassword));
//
////        // Create Kafka source with authenticated Schema Registry deserializer
////        KafkaSource<GenericRecord> source = KafkaSource.<GenericRecord>builder()
////                .setBootstrapServers(bootstrapServers)
////                .setTopics(topic)
////                .setStartingOffsets(OffsetsInitializer.latest())
////                .setValueOnlyDeserializer(
////                        ConfluentRegistryAvroDeserializationSchema.forGeneric(avroSchema, schemaRegistryUrl, schemaRegistryConfigs))
////                .setProperties(kafkaProps)
////                .build();
////
////        // Create stream from Kafka source
////        tableEnv.createTemporaryView("kafka_stream",
////                env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source"));
//
//        tableEnv.executeSql("CREATE TEMPORARY TABLE kafka_stream (\n" +
//                            "    event STRING,\n" +
//                            "    properties STRING\n" +
//                            ") WITH (\n" +
//                            "    'connector' = 'kafka',\n" +
//                            "    'topic' = 'oh-datagen-events-clickstream',\n" +
//                            "    'properties.bootstrap.servers' = 'pkc-ymrq7.us-east-2.aws.confluent.cloud:9092',\n" +
//                            "    'properties.security.protocol' = 'SASL_SSL',\n" +
//                            "    'properties.sasl.mechanism' = 'PLAIN',\n" +
//                            "    'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + kafkaSaslUsername + "\" password=\"" + kafkaSaslPassword + "\";',\n" +
//                            "    'value.format' = 'avro-confluent',\n" +
//                            "    'value.avro-confluent.url' = 'https://psrc-1wydj.us-east-2.aws.confluent.cloud',\n" +
//                            "    'value.avro-confluent.properties.basic.auth.credentials.source' = 'USER_INFO',\n" +
//                            "    'value.avro-confluent.properties.basic.auth.user.info' = '" + schemaRegistryApiKey + ":" + schemaRegistryApiSecret + "',\n" +
//                            "    'scan.startup.mode' = 'earliest-offset'\n" +
//                            ")");
//
//        // Describe schema to validate columns
//        tableEnv.executeSql("DESCRIBE kafka_stream").print();
//        tableEnv.executeSql("SELECT * FROM kafka_stream LIMIT 10").print();
//
//        // Create Hudi table
//        String hudiTable = "hudi_events";
//        tableEnv.executeSql(String.format(
//                "CREATE TABLE IF NOT EXISTS %s (\n" +
//                        "  event STRING,\n" +
//                        "  properties STRING\n" +
//                        ") WITH (\n" +
//                        "  'connector' = 'hudi',\n" +
//                        "  'path' = 's3a://acme-lake/acme/acme_default/sampan-test/hudi_events',\n" +
//                        "  'table.type' = 'MERGE_ON_READ',\n" +
//                        "  'write.tasks' = '1',\n" +
//                        "  'compaction.tasks' = '1'\n" +
//                        ")", hudiTable));
//
//        // Insert data from Kafka into Hudi
//        tableEnv.executeSql(
//                String.format("INSERT INTO %s SELECT event, properties FROM kafka_stream", hudiTable)
//        );
//    }
//
//    private static SchemaRegistryClient getSchemaRegistryClient(String url, String apiKey, String apiSecret) {
//        String credentials = Base64.getEncoder()
//                .encodeToString((apiKey + ":" + apiSecret).getBytes());
//
//        Map<String, String> headers = Map.of(
//                "Authorization", "Basic " + credentials,
//                "Content-Type", "application/vnd.schemaregistry.v1+json"
//        );
//
//        return new CachedSchemaRegistryClient(
//                Arrays.stream(url.split(",")).collect(Collectors.toList()),
//                10,
//                List.of(new AvroSchemaProvider()),
//                null,
//                headers
//        );
//
//    }
//}