import boto3
import socket
from checks.utils.set_logging import logger


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