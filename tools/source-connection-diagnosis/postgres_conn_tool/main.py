import sys
import os

# Add the project root to sys.path
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
if project_root not in sys.path:
    sys.path.append(project_root)

from checks.utils.generic_utils import *
from checks.generic_checks import *
from checks.db_checks import *
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
eks_cluster_name = f'onehouse-customer-cluster-{prefix}'
database_name = args.database_name

# set session
aws_utils = AWSUtils(region)
session = aws_utils.session

# check database infra setup
database_checks = DatabaseChecks(session, database_name)
database_checks.perform_all_database_checks()

generic_checks = GenericChecks(session)
generic_checks.perform_all_generic_checks(database_name=database_name, eks_cluster_name=eks_cluster_name)
