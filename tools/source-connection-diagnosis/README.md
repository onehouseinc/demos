## Description
This tool is used to diagnose the source connection issues for a given source. It will check the connectivity to the sources and the Onehouse cluster.
Currently, this tool supports the following sources:
1. RDS Postgres
2. MSK

## Pre-requisites
* Setup Python 3.9 or above (I've tested with Python 3.10)

## Setup
### Start a virtualenv
```
python3.10 -m venv db-env
source db-env/bin/activate
```

### Install required dependencies
```
python3.10 -m pip install boto3
```

## Run the tool
### Command to run
```
python3.10 <tool_name.py> --region <customer_aws_region> --request-id-prefix <onehouse_request_id_prefix> --database-name <source_db_name>
```

### Sample commands
```
(db-env) sagarl@dev source-connection-diagnosis % python3.10 postgres_conn_tool.py --region us-west-2 --request-id-prefix 123456ab --database-name default-vpc-db
(db-env) sagarl@dev source-connection-diagnosis % python3.10 msk_conn_tool.py --region us-west-2 --request-id-prefix abcdefgh --msk-cluster-arn arn:aws:kafka:us-west-2:1234567890:cluster/cluster_name/123d4567-19d0-482c-bdfc-3c5f0959d3e4-8
```
