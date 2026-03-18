FROM apache/airflow:2.10.5

USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libsasl2-dev \
    libsasl2-modules \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER airflow

# Install providers using Airflow's constraints file to prevent pip from
# upgrading airflow itself (latest hive provider 9.x requires Airflow 3.x)
RUN pip install --no-cache-dir \
    apache-airflow-providers-apache-hive \
    apache-airflow-providers-common-sql \
    "pyhive[hive_pure_sasl]" \
    --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.10.5/constraints-3.12.txt"
