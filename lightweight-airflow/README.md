# Lightweight Airflow for Onehouse SQL

Single-node Airflow running in Docker — a minimal alternative to MWAA for orchestrating Onehouse SQL (Spark Thrift Server).

**Stack:** Airflow 2.10 + LocalExecutor + Postgres. No Celery, no Redis.
**RAM:** ~1.5 GB total at idle.

## Architecture

```
┌─────────────────────────────────────┐
│  Docker Compose                     │
│                                     │
│  ┌──────────────┐  ┌─────────────┐  │
│  │  airflow     │  │  postgres   │  │
│  │  scheduler   │  │  (metadata) │  │
│  │  webserver   │  │             │  │
│  │  LocalExec   │  │             │  │
│  └──────┬───────┘  └─────────────┘  │
│         │ hive2://                  │
└─────────┼───────────────────────────┘
          │ (SSM tunnel on Mac / direct VPC on EC2)
          ▼
    Spark Thrift Server :10000
    (Onehouse SQL)
```

## Quickstart (local Mac)

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env if needed (defaults work with the standard SSM tunnel setup)

# 2. Open a tunnel to Onehouse SQL on port 10000
# Option A — SSM (no SSH key required):
aws ssm start-session \
  --region us-west-2 \
  --target <thrift-server-instance-id> \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["<thrift-server-internal-elb-dns>"],"portNumber":["10000"],"localPortNumber":["10000"]}' &

# Option B — SSH (requires key access to a bastion/jump host):
ssh -N -L 10000:<thrift-server-internal-elb-dns>:10000 <user>@<bastion-host> &

# 3. Build and start
docker compose up --build -d

# 4. Open UI
open http://localhost:8080
# Login: admin / admin (or whatever you set in .env)
```

> Note: DAGs are paused by default on first import. Unpause before triggering:
> ```bash
> docker compose exec scheduler airflow dags unpause onehouse_sql_example
> ```

## Onehouse SQL Connection

Pre-configured via environment variable in `docker-compose.yml`:

```
conn_id: onehouse_sql
type: hiveserver2
host: host.docker.internal   # resolves to your Mac's localhost from inside Docker
port: 10000
auth: NOSASL
```

To override, set `HIVE_HOST`, `HIVE_PORT`, `HIVE_USER`, `HIVE_PASSWORD` in `.env`.

You can also manage connections through the Airflow UI at Admin > Connections.

## Writing DAGs

```python
from airflow.providers.common.sql.operators.sql import SQLExecuteQueryOperator

run_query = SQLExecuteQueryOperator(
    task_id="run_query",
    conn_id="onehouse_sql",
    sql="SELECT COUNT(*) FROM my_db.my_table",
)
```

See `dags/onehouse_sql_example.py` for a full example.

## Useful Commands

```bash
# View logs
docker compose logs -f scheduler

# Stop everything (keeps data)
docker compose down

# Stop and wipe metadata DB
docker compose down -v

# Restart just the scheduler
docker compose restart scheduler

# Trigger a DAG manually
docker compose exec scheduler airflow dags trigger onehouse_sql_example
```

## EC2 Deployment

See [vm/AL_EC2_README.md](vm/AL_EC2_README.md).
