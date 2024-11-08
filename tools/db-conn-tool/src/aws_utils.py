import time

import boto3
import logging
import socket

"""
With this script, you can check the following:
- Glue schema registry availability
- Database availability
- Database parameter group attachment
- Logical replication status
- Logical replication effect
- Database reachability
"""

# logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger()


# set session
def set_session(region: str) -> boto3.Session:
    return boto3.Session(region_name=region)


def get_database_ip_address(session: boto3.Session, database_name: str) -> str | None:
    rds = session.client('rds')
    """
    Retrieve the IP address of the database
    """
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        endpoint_address = response['DBInstances'][0]['Endpoint']['Address']
        # lookup ip address
        ip_address = socket.gethostbyname(endpoint_address)
        return ip_address
    except Exception as e:
        logger.error(f'Error retrieving database IP address: {e}')
        return None


def get_db_eni(session: boto3.Session, ip_address: str) -> str | None:
    ec2 = session.client('ec2')
    """
    Retrieve the ENI ID from the IP address
    """
    try:
        response = ec2.describe_network_interfaces(
            Filters=[
                {'Name': 'association.public-ip', 'Values': [ip_address]}
            ]
        )
        eni_id = response['NetworkInterfaces'][0]['NetworkInterfaceId']
        logger.info(f'Database ENI ID: {eni_id}')
        return eni_id
    except Exception as e:
        logger.error(f'Error retrieving Database ENI ID: {e}')
        return None


def get_eks_cluster_vpc(session: boto3.Session, eks_cluster_name: str) -> str | None:
    eks = session.client('eks')
    """
    Retrieve the security group and VPC ID of the EKS cluster
    """
    try:
        response = eks.describe_cluster(name=eks_cluster_name)
        vpc_id = response['cluster']['resourcesVpcConfig']['vpcId']
        logger.info(f'EKS VPC ID: {vpc_id}')
        return vpc_id
    except Exception as e:
        logger.error(f'Error retrieving EKS cluster security group and VPC: {e}')
        return None


def get_db_instance_vpc(session: boto3.Session, database_name: str) -> str | None:
    """
    Retrieve the security group and VPC ID of the RDS database
    """
    rds = session.client('rds')
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        vpc_id = response['DBInstances'][0]['DBSubnetGroup']['VpcId']
        logger.info(f'DB VPC ID: {vpc_id}')
        return vpc_id
    except Exception as e:
        logger.error(f'Error retrieving DB instance security group and VPC: {e}')
        return None


def check_if_same_vpc(session: boto3.Session, database_name: str, cluster_name: str) -> None:
    """
    Check if the database and the EKS cluster are in the same VPC
    """
    try:
        # Retrieve the security group and VPC ID of the PostgreSQL database
        db_vpc_id = get_db_instance_vpc(session, database_name)
        if not db_vpc_id:
            logger.error('Failed to retrieve DB instance VPC ID ❌')
            return

        # Retrieve the security group and VPC ID of the EKS cluster
        eks_vpc_id = get_eks_cluster_vpc(session, cluster_name)
        if not eks_vpc_id:
            logger.error('Failed to retrieve EKS cluster VPC ID ❌')
            return

        # Check if the VPC IDs are different
        if db_vpc_id != eks_vpc_id:
            logger.warning(f'Database and EKS cluster are in different VPCs: {db_vpc_id} and {eks_vpc_id} 🚧')
        else:
            logger.info(f'Database and EKS cluster are in the same VPC: {db_vpc_id}.')

    except Exception as e:
        logger.error(f'Error retrieving DB/EKS VPC ID {e}')
        return


def get_eks_instance_id(session: boto3.Session, eks_cluster_name: str) -> str | None:
    ec2 = session.client('ec2')
    """
    Retrieve the instance ID of the EKS cluster
    """
    try:
        instance_name = eks_cluster_name
        response = ec2.describe_instances(
            Filters=[
                {
                    'Name': 'tag:Name',
                    'Values': [instance_name]
                }
            ]
        )
        if response['Reservations']:
            instance_info = response['Reservations'][0]['Instances'][0]
            instance_id = instance_info['InstanceId']
            return instance_id
        else:
            print(f"No instance found with name: {instance_name}")
            return None
    except Exception as e:
        print(f"Error describing instance: {e}")
        return None


def setup_reachability_path(session: boto3.Session, database_name: str, cluster_name: str) -> str | None:
    """
    Setup the reachability path between the database and the EKS cluster
    """

    ec2 = session.client('ec2')

    database_ip = get_database_ip_address(session, database_name)
    if not database_ip:
        return None

    db_eni = get_db_eni(session, database_ip)
    if not db_eni:
        return None

    eks_instance_id = get_eks_instance_id(session, cluster_name)
    if not eks_instance_id:
        return None

    try:
        response = ec2.create_network_insights_path(
            Source=eks_instance_id,
            Destination=db_eni,
            Protocol='TCP',
        )
        path_id = response['NetworkInsightsPath']['NetworkInsightsPathId']
        logger.info(f'Network Insights Path ID: {path_id}')
        return path_id
    except Exception as e:
        logger.error(f'Error setting up reachability path: {e}')


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


# database infra checks
def check_database(session: boto3.Session, database_name: str) -> None:
    rds = session.client('rds')
    """
    Check if the database is available
    """
    logger.info(f'Checking database {database_name}...')
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        engine = response['DBInstances'][0]['Engine']
        db_instance_status = response['DBInstances'][0]['DBInstanceStatus']
        if engine in ['postgres', 'aurora-postgresql'] and db_instance_status == 'available':
            logger.info(f'Database {database_name} is available ✅')
            return
        else:
            logger.error(f'Database {database_name} is not available or is not of type postgres/aurora-postgresql ❌')
            return
    except Exception as e:
        logger.error(f'Unable to check if database exists and is available: {e}')


def check_database_parameter_group(session: boto3.Session, database_name: str) -> None:
    rds = session.client('rds')
    """
    Check if the database parameter group is available and is attached to the database
    """
    logger.info(f'Checking database parameter group for {database_name}...')
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        parameter_group_name = response['DBInstances'][0]['DBParameterGroups'][0]['DBParameterGroupName']
        if parameter_group_name:
            logger.info(f'Database parameter group {parameter_group_name} is attached to the database ✅')
            return
        else:
            logger.error('Database parameter group is not attached to the database ❌')
            return
    except Exception as e:
        logger.error(f'Unable to check if the parameter group is available/attached to database: {e}')


def check_logical_replication(session: boto3.Session, database_name: str) -> None:
    rds = session.client('rds')
    """
    Check if the database parameter group has rds.logical_replication enabled
    """
    logger.info(f'Checking logical replication for {database_name}...')
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        parameter_group_name = response['DBInstances'][0]['DBParameterGroups'][0]['DBParameterGroupName']

        all_parameters = []
        marker = None

        # loop to handle pagination
        while True:
            if marker:
                response = rds.describe_db_parameters(DBParameterGroupName=parameter_group_name, Marker=marker)
            else:
                response = rds.describe_db_parameters(DBParameterGroupName=parameter_group_name)
            all_parameters.extend(response['Parameters'])
            if 'Marker' in response:
                marker = response['Marker']
            else:
                break

        for param in all_parameters:
            if param['ParameterName'] == 'rds.logical_replication':
                if param['ParameterValue'] == '1':
                    logger.info(f'Logical replication is enabled in {parameter_group_name} ✅')
                    return
                else:
                    logger.error(f'Logical replication is not enabled in {parameter_group_name} ❌')
                    return
    except Exception as e:
        logger.error(f'Unable to check if logical replication is enabled for the parameter group: {e}')


def check_logical_replication_effect(session: boto3.Session, database_name: str) -> None:
    rds = session.client('rds')
    """
    Check if logical replication has taken effect
    """
    logger.info(f'Checking if logical replication has taken effect in {database_name}...')
    try:
        response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
        parameter_apply_status = response['DBInstances'][0]['DBParameterGroups'][0]['ParameterApplyStatus']
        if parameter_apply_status == 'in-sync':
            logger.info(f'No need for reboot as there no pending-reboot updates in {database_name} ✅')
            return
        elif parameter_apply_status == 'pending-reboot':
            logger.error(
                f'Reboot required to make logical replication changes effective in {database_name}, please reboot ❌')
            return
        else:
            logger.error(f'Logical replication has not taken effect in {database_name} ❌')
            return
    except Exception as e:
        logger.error(f'Unable to check if the logical replication has taken effect in the parameter group: {e}')


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
