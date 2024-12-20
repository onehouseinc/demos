import boto3
import socket
import ipaddress
from .set_logging import logger


class DatabaseUtils:
    def __init__(self, session: boto3.Session):
        self.session = session

    def get_database_ip_address(self, database_name: str) -> str | None:
        rds = self.session.client('rds')
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

    def is_public_ip(self, ip_address: str) -> bool | None:
        """
        Check if the IP address is public
        """
        try:
            ip = ipaddress.ip_address(ip_address)
            return not ip.is_private
        except Exception as e:
            logger.error(f'Error checking if IP address is public: {e}')
            return None

    def get_db_eni(self, ip_address: str) -> str | None:
        ec2 = self.session.client('ec2')
        """
        Retrieve the ENI ID from the IP address
        """
        is_public_ip = self.is_public_ip(ip_address)
        try:
            if is_public_ip:
                response = ec2.describe_network_interfaces(
                    Filters=[
                        {'Name': 'association.public-ip', 'Values': [ip_address]}
                    ]
                )
                eni_id = response['NetworkInterfaces'][0]['NetworkInterfaceId']
                logger.info(f'Database ENI ID: {eni_id}')
                return eni_id
            else:
                response = ec2.describe_network_interfaces(
                    Filters=[
                        {'Name': 'addresses.private-ip-address', 'Values': [ip_address]}
                    ]
                )
            eni_id = response['NetworkInterfaces'][0]['NetworkInterfaceId']
            logger.info(f'Database ENI ID: {eni_id}')
            return eni_id
        except Exception as e:
            logger.error(f'Error retrieving Database ENI ID: {e}')
            return None

    def get_db_instance_vpc(self, database_name: str) -> str | None:
        """
        Retrieve the security group and VPC ID of the RDS database
        """
        rds = self.session.client('rds')
        try:
            response = rds.describe_db_instances(DBInstanceIdentifier=database_name)
            vpc_id = response['DBInstances'][0]['DBSubnetGroup']['VpcId']
            logger.info(f'DB VPC ID: {vpc_id}')
            return vpc_id
        except Exception as e:
            logger.error(f'Error retrieving DB instance security group and VPC: {e}')
            return None
