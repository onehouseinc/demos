from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, MapType, DateType

spark = SparkSession.builder \
    .appName("HudiWriteExample") \
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
    .getOrCreate()
#    .config("spark.sql.defaultCatalog", "default") \
#    .config("spark.sql.catalog.default.defaultDatabase", "gdpr_demo") \
#    .enableHiveSupport() \

catalog = "users"
schema = "name"

# Create table containing sample users
users_schema = StructType([
   StructField('user_id', IntegerType(), False),
   StructField('username', StringType(), True),
   StructField('email', StringType(), True),
   StructField('registration_date', StringType(), True),
   StructField('user_preferences', MapType(StringType(), StringType()), True)
])

users_data = [
   (1, 'alice', 'alice@example.com', '2021-01-01', {'theme': 'dark', 'language': 'en'}),
   (2, 'bob', 'bob@example.com', '2021-02-15', {'theme': 'light', 'language': 'fr'}),
   (3, 'charlie', 'charlie@example.com', '2021-03-10', {'theme': 'dark', 'language': 'es'}),
   (4, 'david', 'david@example.com', '2021-04-20', {'theme': 'light', 'language': 'de'}),
   (5, 'eve', 'eve@example.com', '2021-05-25', {'theme': 'dark', 'language': 'it'})
]

users_df = spark.createDataFrame(users_data, schema=users_schema)
users_df.write \
    .format("hudi") \
    .option("hoodie.datasource.write.recordkey.field", "user_id") \
    .option("hoodie.datasource.write.precombine.field", "user_id") \
    .option("hoodie.datasource.write.table.type", "MERGE_ON_READ") \
    .option("hoodie.table.name", "source_users") \
    .option("path", "s3://lakehouse-albert-us-west-2/demolake/gdpr_demo/source_users") \
    .mode("overwrite") \
    .saveAsTable("gdpr_demo.source_users")

# Create table containing clickstream (i.e. user activities)
from pyspark.sql.types import TimestampType

clicks_schema = StructType([
   StructField('click_id', IntegerType(), False),
   StructField('user_id', IntegerType(), True),
   StructField('url_clicked', StringType(), True),
   StructField('click_timestamp', StringType(), True),
   StructField('device_type', StringType(), True),
   StructField('ip_address', StringType(), True)
])

clicks_data = [
   (1001, 1, 'https://example.com/home', '2021-06-01T12:00:00', 'mobile', '192.168.1.1'),
   (1002, 1, 'https://example.com/about', '2021-06-01T12:05:00', 'desktop', '192.168.1.1'),
   (1003, 2, 'https://example.com/contact', '2021-06-02T14:00:00', 'tablet', '192.168.1.2'),
   (1004, 3, 'https://example.com/products', '2021-06-03T16:30:00', 'mobile', '192.168.1.3'),
   (1005, 4, 'https://example.com/services', '2021-06-04T10:15:00', 'desktop', '192.168.1.4'),
   (1006, 5, 'https://example.com/blog', '2021-06-05T09:45:00', 'tablet', '192.168.1.5')
]

clicks_df = spark.createDataFrame(clicks_data, schema=clicks_schema)
clicks_df.write \
    .format("hudi") \
    .option("hoodie.datasource.write.recordkey.field", "click_id") \
    .option("hoodie.datasource.write.precombine.field", "click_id") \
    .option("hoodie.datasource.write.table.type", "MERGE_ON_READ") \
    .option("hoodie.table.name", "source_clicks") \
    .option("hoodie.datasource.meta_sync.condition.sync","true") \
    .mode("overwrite") \
    .saveAsTable("gdpr_demo.source_clicks")
