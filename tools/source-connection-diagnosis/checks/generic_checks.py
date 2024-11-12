import boto3
import time
from checks.utils.generic_utils import *


class GenericChecks:
    def __init__(self, session: boto3.Session):
        self.session = session
        self.glue = self.session.client('glue')
        self.ec2 = self.session.client('ec2')
        self.aws_utils = AWSUtils(self.session.region_name)

    def check_glue_schema_registry(self) -> None:
        """
        Check if there is a valid Glue schema registry
        """
        logger.info('Checking Glue schema registry...')
        try:
            response = self.glue.list_registries()
            if len(response['Registries']) > 0:
                logger.info('At least one Glue schema registry is available ✅')
            else:
                logger.error('No Glue schema registry available ❌')
        except Exception as e:
            logger.error(f'Unable to retrieve schema registry: {e}')

    def check_database_reachability(self, database_name: str, cluster_name: str) -> None:
        """
        Check if the database is reachable from the EKS cluster
        """
        self.aws_utils.check_if_db_vpc_equals_eks_vpc(database_name, cluster_name)

        logger.info(f'Checking database reachability from {cluster_name}...')
        path_id = self.aws_utils.setup_reachability_path(database_name, cluster_name)
        if path_id:
            try:
                start_nia_response = self.ec2.start_network_insights_analysis(NetworkInsightsPathId=path_id)
                logger.info(f'Network Insights analysis started for path {path_id}')
                nia_id = start_nia_response['NetworkInsightsAnalysis']['NetworkInsightsAnalysisId']
                logger.info(f'Network Insights Analysis ID: {nia_id}')
                logger.info("Started Network Insights Analysis, sleeping for 10s before polling again...")
                time.sleep(10)
                describe_nia_response = self.ec2.describe_network_insights_analyses(NetworkInsightsAnalysisIds=[nia_id])
                nia_status = describe_nia_response['NetworkInsightsAnalyses'][0]['Status']
                # if status is running, poll every 10 seconds until it's succeeded or failed
                while nia_status == 'running':
                    logger.info("Network Insights Analysis is still running, sleeping for 10s before polling again...")
                    time.sleep(10)
                    describe_nia_response = self.ec2.describe_network_insights_analyses(NetworkInsightsAnalysisIds=[nia_id])
                    nia_status = describe_nia_response['NetworkInsightsAnalyses'][0]['Status']
                nia_network_path_found_flag = describe_nia_response['NetworkInsightsAnalyses'][0]['NetworkPathFound']
                logger.info(f'Network Insights Analysis status: {nia_status}')
                logger.info(f'Network Path Found: {nia_network_path_found_flag}')
                if nia_status == 'succeeded' and nia_network_path_found_flag:
                    logger.info(f'Database {database_name} is reachable from {cluster_name} ✅')
                else:
                    explanation = describe_nia_response['NetworkInsightsAnalyses'][0]['Explanations']
                    logger.error(f'Database {database_name} is not reachable from {cluster_name} ❌')
                    logger.error(f'Explanations: {explanation}')
            except Exception as e:
                logger.error(f'Error checking database reachability: {e}')
        else:
            logger.error(f'Database {database_name} is not reachable from {cluster_name} ❌')

    def perform_all_generic_checks(self, database_name: str, cluster_name: str) -> None:
        self.check_glue_schema_registry()
        self.check_database_reachability(database_name, cluster_name)