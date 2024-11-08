import src.aws_utils as utils
import argparse
import logging

# configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger()

# get args from the user
parser = argparse.ArgumentParser()
parser.add_argument('--region', type=str, help='AWS region', required=True)
parser.add_argument('--request-id-prefix', type=str, help='Onehouse Request ID prefix', required=True)
parser.add_argument('--database-name', type=str, help='Postgres Database name', required=True)
args = parser.parse_args()

region = args.region
prefix = args.request_id_prefix
cluster_name = f'onehouse-customer-cluster-{prefix}'
database_name = args.database_name

# set session
session = utils.set_session(region)

# check glue schema registry
utils.check_glue_schema_registry(session)

# check database infra setup
utils.check_database(session, database_name)
utils.check_database_parameter_group(session, database_name)
utils.check_logical_replication(session, database_name)
utils.check_logical_replication_effect(session, database_name)
utils.check_database_reachability(session, database_name, cluster_name)
