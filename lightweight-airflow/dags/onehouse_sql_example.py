"""
Example DAG: Run SQL against Onehouse SQL (Spark Thrift Server).

Connection used: AIRFLOW_CONN_ONEHOUSE_SQL
  hiveserver2://hive:hive@host.docker.internal:10000/default?auth=NOSASL

Prerequisites:
  - SSM tunnel forwarding port 10000 to the Spark Thrift Server must be active
  - See .env.example for connection configuration
"""

from datetime import datetime, timedelta

from airflow import DAG
from airflow.providers.common.sql.operators.sql import SQLExecuteQueryOperator

# To log query results directly in the task log, use a PythonOperator with
# HiveServer2Hook instead of SQLExecuteQueryOperator:
#
# import logging
# from airflow.operators.python import PythonOperator
# from airflow.providers.apache.hive.hooks.hive import HiveServer2Hook
#
# def run_and_log(sql, conn_id="onehouse_sql"):
#     log = logging.getLogger(__name__)
#     hook = HiveServer2Hook(hiveserver2_conn_id=conn_id)
#     cursor = hook.get_conn().cursor()
#     cursor.execute(sql)
#     rows = cursor.fetchall()
#     cols = [d[0] for d in cursor.description] if cursor.description else []
#     log.info("columns: %s", cols)
#     for row in rows:
#         log.info("row: %s", row)
#     log.info("total rows: %d", len(rows))
#     return rows
#
# show_databases = PythonOperator(
#     task_id="show_databases",
#     python_callable=run_and_log,
#     op_kwargs={"sql": "SHOW DATABASES"},
# )

default_args = {
    "owner": "onehouse",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="onehouse_sql_example",
    default_args=default_args,
    description="Example: run SQL queries against Onehouse SQL",
    schedule="@daily",
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["onehouse", "sql"],
) as dag:

    # Simple connectivity check
    check_connection = SQLExecuteQueryOperator(
        task_id="check_connection",
        conn_id="onehouse_sql",
        sql="SELECT 1",
    )

    # List databases
    show_databases = SQLExecuteQueryOperator(
        task_id="show_databases",
        conn_id="onehouse_sql",
        sql="SHOW DATABASES",
    )

    # Example: run a real query — swap in your table name
    # sample_query = SQLExecuteQueryOperator(
    #     task_id="sample_query",
    #     conn_id="onehouse_sql",
    #     sql="SELECT COUNT(*) FROM my_database.my_table",
    # )

    check_connection >> show_databases
