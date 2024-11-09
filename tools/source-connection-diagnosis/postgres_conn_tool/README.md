
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
python3.10 app.py --region <customer_aws_region> --request-id-prefix <onehouse_request_id_prefix> --database-name <source_db_name>
```

### Sample run
```
(db-env) sagarl@dev db-conn-tool % python3.10 app.py --region us-west-2 --request-id-prefix 739935cf --database-name ajax-vpc-db
```

## Understanding the output
#### When the EKS cluster and database exist in same VPC and setup correctly
<details>
<summary>Click to expand</summary>

```
(db-env) sagarl@dev db-conn-tool % python3.10 app.py --region us-west-2 --request-id-prefix 739935cf --database-name ajax-vpc-db          
2024-11-08 10:38:08 - botocore.credentials - INFO - Found credentials in shared credentials file: ~/.aws/credentials
2024-11-08 10:38:09 - root - INFO - Checking Glue schema registry...
2024-11-08 10:38:09 - root - INFO - At least one Glue schema registry is available ✅
2024-11-08 10:38:09 - root - INFO - Checking database ajax-vpc-db...
2024-11-08 10:38:09 - root - INFO - Database ajax-vpc-db is available ✅
2024-11-08 10:38:09 - root - INFO - Checking database parameter group for ajax-vpc-db...
2024-11-08 10:38:09 - root - INFO - Database parameter group postgres16-oh is attached to the database ✅
2024-11-08 10:38:09 - root - INFO - Checking logical replication for ajax-vpc-db...
2024-11-08 10:38:10 - root - INFO - Logical replication is enabled in postgres16-oh ✅
2024-11-08 10:38:10 - root - INFO - Checking if logical replication has taken effect in ajax-vpc-db...
2024-11-08 10:38:10 - root - INFO - No need for reboot as there no pending-reboot updates in ajax-vpc-db ✅
2024-11-08 10:38:10 - root - INFO - DB VPC ID: vpc-07bf185efa6b28ac6
2024-11-08 10:38:11 - root - INFO - EKS VPC ID: vpc-07bf185efa6b28ac6
2024-11-08 10:38:11 - root - INFO - Database and EKS cluster are in the same VPC: vpc-07bf185efa6b28ac6.
2024-11-08 10:38:11 - root - INFO - Checking database reachability from onehouse-customer-cluster-739935cf...
2024-11-08 10:38:11 - root - INFO - Database ENI ID: eni-0d50652bc67d0ca1e
2024-11-08 10:38:12 - root - INFO - Network Insights Path ID: nip-05eed0ebab757d053
2024-11-08 10:38:13 - root - INFO - Network Insights analysis started for path nip-05eed0ebab757d053
2024-11-08 10:38:13 - root - INFO - Network Insights Analysis ID: nia-0e5fd736d0391b1ea
2024-11-08 10:38:13 - root - INFO - Started Network Insights Analysis, sleeping for 10s before polling again...
2024-11-08 10:38:24 - root - INFO - Network Insights Analysis is still running, sleeping for 10s before polling again...
2024-11-08 10:38:34 - root - INFO - Network Insights Analysis is still running, sleeping for 10s before polling again...
2024-11-08 10:38:44 - root - INFO - Network Insights Analysis status: succeeded
2024-11-08 10:38:44 - root - INFO - Network Path Found: True
2024-11-08 10:38:44 - root - INFO - Database ajax-vpc-db is reachable from onehouse-customer-cluster-739935cf ✅
```
</details>

#### When the EKS cluster and database exist in different VPC and peering connection is setup correctly
<details>
<summary>Click to expand</summary>

```
(db-env) sagarl@dev postgres-conn-tool % python3.10 app.py --region us-west-2 --request-id-prefix 739935cf --database-name default-vpc-db-sagarl
2024-11-08 11:39:44 - botocore.credentials - INFO - Found credentials in shared credentials file: ~/.aws/credentials
2024-11-08 11:39:44 - root - INFO - Checking Glue schema registry...
2024-11-08 11:39:44 - root - INFO - At least one Glue schema registry is available ✅
2024-11-08 11:39:44 - root - INFO - Checking database default-vpc-db-sagarl...
2024-11-08 11:39:45 - root - INFO - Database default-vpc-db-sagarl is available ✅
2024-11-08 11:39:45 - root - INFO - Checking database parameter group for default-vpc-db-sagarl...
2024-11-08 11:39:45 - root - INFO - Database parameter group postgres16-oh is attached to the database ✅
2024-11-08 11:39:45 - root - INFO - Checking logical replication for default-vpc-db-sagarl...
2024-11-08 11:39:46 - root - INFO - Logical replication is enabled in postgres16-oh ✅
2024-11-08 11:39:46 - root - INFO - Checking if logical replication has taken effect in default-vpc-db-sagarl...
2024-11-08 11:39:46 - root - INFO - No need for reboot as there no pending-reboot updates in default-vpc-db-sagarl ✅
2024-11-08 11:39:46 - root - INFO - DB VPC ID: vpc-01edd1cf495f0b63e
2024-11-08 11:39:47 - root - INFO - EKS VPC ID: vpc-07bf185efa6b28ac6
2024-11-08 11:39:47 - root - WARNING - Database and EKS cluster are in different VPCs: vpc-01edd1cf495f0b63e and vpc-07bf185efa6b28ac6 🚧
2024-11-08 11:39:47 - root - INFO - Checking database reachability from onehouse-customer-cluster-739935cf...
2024-11-08 11:39:47 - root - INFO - Database ENI ID: eni-039282c3b5cba2e3f
2024-11-08 11:39:48 - root - INFO - Network Insights Path ID: nip-013008a3fbe32f5d6
2024-11-08 11:39:49 - root - INFO - Network Insights analysis started for path nip-013008a3fbe32f5d6
2024-11-08 11:39:49 - root - INFO - Network Insights Analysis ID: nia-0a2d90571a05f3f5d
2024-11-08 11:39:49 - root - INFO - Started Network Insights Analysis, sleeping for 10s before polling again...
2024-11-08 11:39:59 - root - INFO - Network Insights Analysis is still running, sleeping for 10s before polling again...
2024-11-08 11:40:10 - root - INFO - Network Insights Analysis status: succeeded
2024-11-08 11:40:10 - root - INFO - Network Path Found: True
2024-11-08 11:40:10 - root - INFO - Database default-vpc-db-sagarl is reachable from onehouse-customer-cluster-739935cf ✅
```
</details>

#### When the EKS cluster and database exist in different VPC and peering connection is setup incorrectly
<details>
<summary>Click to expand</summary>

```
.
.
.
2024-11-08 10:51:01 - root - INFO - Network Insights Analysis status: succeeded
2024-11-08 10:51:01 - root - INFO - Network Path Found: False
2024-11-08 10:51:01 - root - ERROR - Database default-vpc-db-sagarl is not reachable from onehouse-customer-cluster-739935cf ❌
2024-11-08 10:51:01 - root - ERROR - Explanations: [{'Destination': {'Id': 'pcx-0422cea747ae05ec9', 'Arn': 'arn:aws:ec2:us-west-2:654654235321:vpc-peering-connection/pcx-0422cea747ae05ec9'}, 'ExplanationCode': 'NO_ROUTE_TO_DESTINATION', 'RouteTable': {'Id': 'rtb-0384f68036a719718', 'Arn': 'arn:aws:ec2:us-west-2:654654235321:route-table/rtb-0384f68036a719718'}, 'Vpc': {'Id': 'vpc-07bf185efa6b28ac6', 'Arn': 'arn:aws:ec2:us-west-2:654654235321:vpc/vpc-07bf185efa6b28ac6'}}]

```
</details>

#### When `rds.logical_replication` is not enabled
<details>
<summary>Click to expand</summary>

```
.
.
.
2024-09-10 16:42:57 - root - INFO - Checking logical replication for database-3...
2024-09-10 16:42:58 - root - ERROR - Logical replication is not enabled in default.postgres16 ❌
.
.
.
```
</details>

#### When `rds.logical_replication` is enabled but database requires a reboot
<details>
<summary>Click to expand</summary>

```
.
.
.
2024-09-10 16:56:54 - root - INFO - Checking if logical replication has taken effect in database-3...
2024-09-10 16:56:54 - root - ERROR - Reboot required to make logical replication changes effective in database-3, please reboot ❌
.
.
.
```
</details>