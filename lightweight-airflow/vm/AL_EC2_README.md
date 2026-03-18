# Deploying on Amazon Linux 2023 EC2

## Prerequisites

- EC2 instance running **Amazon Linux 2023**
- Instance in the same VPC as the Onehouse SQL Spark Thrift Server
- Security group rules:
  - Outbound port `10000` to the Thrift Server's security group
  - Inbound port `8080` not required — access the UI via SSM port forward (see below)
- Recommended instance size: `t2.medium` or `t3.medium` (4 GB RAM)

## Bootstrap (fresh instance)

Copy the `lightweight-airflow/` folder onto the instance (e.g. via `scp` or by pulling from the repo), then run from the `vm/` directory:

```bash
sudo HIVE_HOST="<internal-thrift-server-dns>" \
     AIRFLOW_PASSWORD="<strong-password>" \
     bash al_ec2.sh
```

The script will:
1. Install Docker and Docker Compose
2. Install Docker Buildx (required for `docker compose build`)
3. Add `ec2-user` and `ssm-user` to the `docker` group
4. Write `.env` with the provided values
5. Build and start the stack

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `HIVE_HOST` | internal ELB DNS | Internal DNS/IP of the Spark Thrift Server |
| `HIVE_PORT` | `10000` | Thrift Server port |
| `HIVE_USER` | `hive` | Thrift Server user |
| `HIVE_PASSWORD` | `hive` | Thrift Server password |
| `AIRFLOW_USER` | `admin` | Airflow UI username |
| `AIRFLOW_PASSWORD` | `admin` | Airflow UI password — **change this** |
| `AIRFLOW_SECRET_KEY` | random | Webserver secret key — auto-generated if not set |

## Accessing the UI

**Option A — SSM (no SSH key required):**

```bash
aws ssm start-session \
  --region us-west-2 \
  --target <instance-id> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["8080"],"localPortNumber":["8080"]}'
```

**Option B — SSH tunnel (requires key access):**

```bash
ssh -N -L 8080:localhost:8080 <user>@<instance-ip-or-bastion>
```

Then open `http://localhost:8080`.

## After bootstrap

```bash
cd lightweight-airflow

# Check containers
docker compose ps

# Follow logs
docker compose logs -f

# Trigger a DAG manually
docker compose exec scheduler airflow dags trigger onehouse_sql_example
```

> DAGs are paused by default on first import. Unpause before triggering:
> ```bash
> docker compose exec scheduler airflow dags unpause onehouse_sql_example
> ```

## Deploying DAG updates

```bash
git pull
# The scheduler picks up DAG file changes automatically — no restart needed.
# For docker-compose.yml or Dockerfile changes:
docker compose up --build -d
```

## If containers don't come back after a reboot

Docker and the containers are configured to start on boot, but if Postgres doesn't come up in time the other containers may start without it. Just run:

```bash
cd lightweight-airflow && docker compose up -d
```

## Notes

- The `.env` file lives in the `lightweight-airflow/` root and is gitignored
- Postgres data is stored in a Docker volume on the instance's EBS root volume — if you replace the instance, DAG run history is lost. Acceptable for a demo/lightweight setup.
