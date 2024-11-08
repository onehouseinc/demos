
import ai.onehouse.transformer.SqlQueryBasedTransformer;
import org.apache.hudi.common.config.TypedProperties;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hudi.common.config.TypedProperties;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.hadoop.fs.FileUtil;


/**
 * Unit test for simple App.
 */
class SqlQueryBasedTransformerTest {

    private static SparkSession spark;
    private static JavaSparkContext jsc;
    private static Dataset<Row> inputDF;

    //private Logger LOG = LogManager.getLogger("SqlQueryBasedTransformerTest.class");

 /*    @BeforeAll
    static void setUp() {

        spark = SparkSession.builder().appName("SqlQueryBasedTransformerTest").master("local").getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());

        String tempDir = spark.conf().get("spark.sql.warehouse.dir");
        boolean success = FileUtil.fullyDelete(new java.io.File(tempDir),true);
        System.out.println("deleted: " + tempDir + success);

        TypedProperties properties = new TypedProperties();
        properties.put("sql", "CREATE TABLE if not exists student (id INT, name STRING, age INT) USING CSV;");         
        SqlQueryBasedTransformer transformer = new SqlQueryBasedTransformer();
        Dataset<Row> outputDF = transformer.apply(jsc, spark, inputDF, properties);    

        properties.put("sql", "INSERT INTO student VALUES (1, 'Ryan', 9);");        
        outputDF = transformer.apply(jsc, spark, inputDF, properties);    

    }

    @AfterAll
    static void tearDown() {
        spark.close();
    }




    @Test
    public void testApply() {


        TypedProperties properties = new TypedProperties();
        properties.put("sql", "ALTER TABLE student ADD COLUMNS comment STRING;");

        SqlQueryBasedTransformer transformer = new SqlQueryBasedTransformer();
        Dataset<Row> outputDF = transformer.apply(jsc, spark, inputDF, properties);

        TypedProperties properties2 = new TypedProperties();
        properties2.put("sql", "select * from student;");

        SqlQueryBasedTransformer transformer2 = new SqlQueryBasedTransformer();
        Dataset<Row> outputDF2 = transformer2.apply(jsc, spark, inputDF, properties2);        

         // construct the expected dataframe
        List<Row> expectedData = Arrays.asList(
        //               RowFactory.create(2, "Albert", 21),
        RowFactory.create(1, "Ryan", 9, null)
        );
        StructType expectedSchema = new StructType(new StructField[]{
                new StructField("id", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("name", DataTypes.StringType, false, Metadata.empty()),
                new StructField("age", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("comment", DataTypes.StringType, true, Metadata.empty())                
        });
        Dataset<Row> expectedDF = spark.createDataFrame(expectedData, expectedSchema);

        //Assertions.assertEquals(expectedDF.collectAsList(), outputDF.collectAsList());
        Assertions.assertEquals(expectedDF.collectAsList(), outputDF2.collectAsList());
    }

 */    
}
