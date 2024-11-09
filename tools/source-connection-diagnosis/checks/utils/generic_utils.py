import boto3
from checks.utils.db_utils import *
from checks.utils.set_logging import *


# set session
def set_session(region: str) -> boto3.Session:
    return boto3.Session(region_name=region)


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
