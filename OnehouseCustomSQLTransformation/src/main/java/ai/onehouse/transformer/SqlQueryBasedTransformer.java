/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package ai.onehouse.transformer;

 import org.apache.hudi.utilities.transform.Transformer;
 import org.apache.hudi.common.config.TypedProperties;
 import org.apache.hudi.utilities.config.SqlTransformerConfig;
 import org.apache.hudi.utilities.exception.HoodieTransformExecutionException;
 
 import org.apache.spark.api.java.JavaSparkContext;
 import org.apache.spark.sql.Dataset;
 import org.apache.spark.sql.Row;
 import org.apache.spark.sql.SparkSession;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.glue.GlueClient;

import java.util.UUID;
 
 import static org.apache.hudi.common.util.ConfigUtils.getStringWithAltKeys;
/**
 * A transformer that allows a sql-query template be used to transform the source before writing to Hudi data-set.
 *
 * The query should reference the source as a table named "\<SRC\>"
 */
public class SqlQueryBasedTransformer implements Transformer {

  private static final Logger LOG = LoggerFactory.getLogger(SqlQueryBasedTransformer.class);

  private static final String SRC_PATTERN = "<SRC>";
  private static final String TMP_TABLE = "HOODIE_SRC_TMP_TABLE_";

  @Override
  public Dataset<Row> apply(JavaSparkContext jsc, SparkSession sparkSession, Dataset<Row> rowDataset,
      TypedProperties properties) {
    String transformerSQL = getStringWithAltKeys(properties, SqlTransformerConfig.TRANSFORMER_SQL);



    /*  
    33-thread-4} Registering tmp table: HOODIE_SRC_TMP_TABLE_7e8e98da_fb77_4334_9fdb_8957c5d6e7b1 
    2024-08-16T03:19:14.192Z INFO ai.onehouse.transformer.SqlQueryBasedTransformer {database.table=sales_default.public_orders} {thread_id=177} {thread_name=pool-33-thread-4} SQL Database/Table/Current Database List for transformation: +-------+----------------+----------------------------------------+
    |name   |description     |locationUri                             |
    +-------+----------------+----------------------------------------+
    |default|default database|file:/opt/spark/work-dir/spark-warehouse|
    +-------+----------------+----------------------------------------+
     +---------------------------------------------------------+--------+-----------+---------+-----------+
    |name                                                     |database|description|tableType|isTemporary|
    +---------------------------------------------------------+--------+-----------+---------+-----------+
    |hoodie_src_tmp_table_5a8eb33c_f0a8_4730_abbf_b9d7449e817c|null    |null       |TEMPORARY|true       |
    |hoodie_src_tmp_table_7e8e98da_fb77_4334_9fdb_8957c5d6e7b1|null    |null       |TEMPORARY|true       |
    |hoodie_src_tmp_table_87a7c4e1_57ca_450e_806d_d3f1cf7da150|null    |null       |TEMPORARY|true       |
    |hoodie_src_tmp_table_fe84c205_07c3_479f_9b91_ecacfd40a073|null    |null       |TEMPORARY|true       |
    +---------------------------------------------------------+--------+-----------+---------+-----------+
     default  
     */

     // if not using quarantine (_event_lsn is required by debezium)  
     // hoodie.streamer.transformer.sql=select toy_id, name, price, '01-02-20' as date, CAST(price AS String) AS price_string, _event_lsn from <SRC>

     // iff using quarantine (_corrupt_record is required by quarantine, _event_lsn is required by debezium)
     // hoodie.streamer.transformer.sql=select toy_id, name, price, '01-02-20' as date, CAST(price AS String) AS price_string, _event_lsn, _corrupt_record from <SRC>

    try {
      // tmp table name doesn't like dashes
      String tmpTable = TMP_TABLE.concat(UUID.randomUUID().toString().replace("-", "_"));
      LOG.info("Registering tmp table: {}", tmpTable);

      //createOrReplaceTempView per API docs is not tied to any catalog.  Must use createOrReplaceGlobalTempView.
      rowDataset.createOrReplaceTempView(tmpTable);
      //rowDataset.createOrReplaceGlobalTempView(tmpTable);

      LOG.info("SQL Database/Table/Current Database List for transformation: {} {} {}", sparkSession.catalog().listDatabases().showString(10,0,false), sparkSession.catalog().listTables().showString(10,0,false), sparkSession.catalog().currentDatabase());
      String sqlStr = transformerSQL.replaceAll(SRC_PATTERN, tmpTable);
      LOG.info("SQL Query for transformation: {}", sqlStr);
      Dataset<Row> transformed = sparkSession.sql(sqlStr);

      sparkSession.catalog().dropTempView(tmpTable);
      //sparkSession.catalog().dropGlobalTempView(tmpTable);

      return transformed;
    } catch (Exception e) {
      throw new HoodieTransformExecutionException("Failed to apply sql query based transformer", e);
    }
  }
}