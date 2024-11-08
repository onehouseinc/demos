
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
### Sample run
```
(db-env) sagarl@dev db-conn-tool % python3.10 app.py --region us-west-2 --request-id-prefix 739935cf --database-name ajax-vpc-db
```

## Understanding the output
#### When the EKS cluster and database exist in same VPC
<details>
<summary>Click to expand</summary>

```
(db-env) sagarl@dev db-conn-tester % python3.10 app.py --region us-west-2 --request-id-prefix 739935cf --database-name database-1
2024-09-10 16:58:29 - botocore.credentials - INFO - Found credentials in shared credentials file: ~/.aws/credentials
2024-09-10 16:58:30 - root - INFO - Checking Glue schema registry...
2024-09-10 16:58:30 - root - INFO - Atleast one Glue schema registry is available ✅
2024-09-10 16:58:30 - root - INFO - Checking database database-1...
2024-09-10 16:58:30 - root - INFO - Database database-1 is available ✅
2024-09-10 16:58:30 - root - INFO - Checking database parameter group for database-1...
2024-09-10 16:58:30 - root - INFO - Database parameter group postgres16-oh is attached to the database ✅
2024-09-10 16:58:30 - root - INFO - Checking logical replication for database-1...
2024-09-10 16:58:31 - root - INFO - Logical replication is enabled in postgres16-oh ✅
2024-09-10 16:58:31 - root - INFO - Checking if logical replication has taken effect in database-1...
2024-09-10 16:58:31 - root - INFO - No need for reboot as there no pending-reboot updates database-1 ✅
2024-09-10 16:58:31 - root - INFO - Checking database reachability from EKS cluster onehouse-customer-cluster-739935cf...
2024-09-10 16:58:31 - root - INFO - DB Security group ID: sg-0f8f36e0df98ed39e, DB VPC ID: vpc-07bf185efa6b28ac6
2024-09-10 16:58:31 - root - INFO - EKS Security group ID: sg-094c4e947e0ae2813, EKS VPC ID: vpc-07bf185efa6b28ac6
2024-09-10 16:58:31 - root - INFO - Database and EKS cluster are in the same VPC: vpc-07bf185efa6b28ac6. Checking security group rules...
2024-09-10 16:58:31 - root - INFO - Database security group sg-0f8f36e0df98ed39e allows inbound traffic from EKS cluster security group sg-094c4e947e0ae2813 on port 5432 ✅
```
</details>

#### When the EKS cluster and database exist in different VPC and peering connection is setup correctly
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