import boto3
import socket
from .set_logging import logger


class MSKUtils:
    def __init__(self, session: boto3.Session):
        self.session = session
        self.msk = self.session.client('kafka')
        self.ec2 = self.session.client('ec2')

    def get_msk_ip_address(self, msk_cluster_arn: str) -> str | None:
        try:
            response = self.msk.get_bootstrap_brokers(ClusterArn=msk_cluster_arn)
            bootstrap_broker_string = response['BootstrapBrokerStringSaslIam']
            first_bootstrap_broker_endpoint = bootstrap_broker_string.split(',')[1].split(':')[0]
            # lookup ip address
            ip_address = socket.gethostbyname(first_bootstrap_broker_endpoint)
            return ip_address
        except Exception as e:
            logger.error(f'Error retrieving MSK IP address: {e}')
            return None

    def get_msk_eni(self, msk_cluster_arn: str) -> str | None:
        """
        Retrieve the ENI ID from the IP address
        """
        ip_address = self.get_msk_ip_address(msk_cluster_arn)
        try:
            response = self.ec2.describe_network_interfaces(
                Filters=[
                    {'Name': 'addresses.private-ip-address', 'Values': [ip_address]}
                ]
            )
            eni_id = response['NetworkInterfaces'][0]['NetworkInterfaceId']
            logger.info(f'MSK ENI ID: {eni_id}')
            return eni_id
        except Exception as e:
            logger.error(f'Error retrieving MSK ENI ID: {e}')
            return None

    def get_msk_cluster_vpc(self, msk_cluster_arn: str) -> str | None:
        """
        Retrieve the VPC ID of the MSK cluster
        """
        try:
            response = self.msk.describe_cluster(ClusterArn=msk_cluster_arn)
            subnet_id_list = response['ClusterInfo']['BrokerNodeGroupInfo']['ClientSubnets']
            vpc_id = self.ec2.describe_subnets(SubnetIds=subnet_id_list)['Subnets'][0]['VpcId']
            logger.info(f'MSK VPC ID: {vpc_id}')
            return vpc_id
        except Exception as e:
            logger.error(f'Error retrieving MSK cluster VPC: {e}')
            return None

