from checks.utils.generic_utils import logger
from checks.utils.generic_utils import check_if_same_vpc, setup_reachability_path
import boto3
import time


def check_glue_schema_registry(session: boto3.Session) -> None:
    glue = session.client('glue')
    """
    Check if there is a valid Glue schema registry
    """
    logger.info('Checking Glue schema registry...')
    try:
        response = glue.list_registries()
        if len(response['Registries']) > 0:
            logger.info('At least one Glue schema registry is available ✅')
            return
        else:
            logger.error('No Glue schema registry available ❌')
            return
    except Exception as e:
        logger.error(f'Unable to retrieve schema registry: {e}')


def check_database_reachability(session: boto3.Session, database_name: str, cluster_name: str) -> None:
    """
    Check if the database is reachable from the EKS cluster
    """
    ec2 = session.client('ec2')

    check_if_same_vpc(session, database_name, cluster_name)

    logger.info(f'Checking database reachability from {cluster_name}...')
    path_id = setup_reachability_path(session, database_name, cluster_name)
    if path_id:
        try:
            start_nia_response = ec2.start_network_insights_analysis(NetworkInsightsPathId=path_id)
            logger.info(f'Network Insights analysis started for path {path_id}')
            nia_id = start_nia_response['NetworkInsightsAnalysis']['NetworkInsightsAnalysisId']
            logger.info(f'Network Insights Analysis ID: {nia_id}')
            logger.info("Started Network Insights Analysis, sleeping for 10s before polling again...")
            time.sleep(10)
            describe_nia_response = ec2.describe_network_insights_analyses(NetworkInsightsAnalysisIds=[nia_id])
            nia_status = describe_nia_response['NetworkInsightsAnalyses'][0]['Status']
            # if status is running, poll every 10 seconds until it's succeeded or failed
            while nia_status == 'running':
                logger.info("Network Insights Analysis is still running, sleeping for 10s before polling again...")
                time.sleep(10)
                describe_nia_response = ec2.describe_network_insights_analyses(NetworkInsightsAnalysisIds=[nia_id])
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