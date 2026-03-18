from .db_utils import *
from .msk_utils import *
from .set_logging import *


class AWSUtils:
    def __init__(self, region: str):
        self.session = boto3.Session(region_name=region)
        self.db_utils = DatabaseUtils(self.session)
        self.msk_utils = MSKUtils(self.session)

    def get_eks_cluster_vpc(self, eks_cluster_name: str) -> str | None:
        eks = self.session.client('eks')
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

    def get_eks_instance_id(self, eks_cluster_name: str) -> str | None:
        ec2 = self.session.client('ec2')
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

    def check_if_db_vpc_equals_eks_vpc(self, database_name: str, eks_cluster_name: str) -> None:
        """
        Check if the database and the EKS cluster are in the same VPC
        """
        try:
            # Retrieve the security group and VPC ID of the PostgreSQL database
            db_vpc_id = self.db_utils.get_db_instance_vpc(database_name)
            if not db_vpc_id:
                logger.error('Failed to retrieve DB instance VPC ID ❌')
                return

            # Retrieve the security group and VPC ID of the EKS cluster
            eks_vpc_id = self.get_eks_cluster_vpc(eks_cluster_name)
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

    def check_if_msk_vpc_equals_eks_vpc(self, msk_cluster_arn: str, eks_cluster_name: str) -> None:
        """
        Check if the database and the EKS cluster are in the same VPC
        """
        try:
            # Retrieve the security group and VPC ID of the PostgreSQL database
            msk_vpc_id = self.msk_utils.get_msk_cluster_vpc(msk_cluster_arn)
            if not msk_vpc_id:
                logger.error('Failed to retrieve MSK cluster VPC ID ❌')
                return

            # Retrieve the security group and VPC ID of the EKS cluster
            eks_vpc_id = self.get_eks_cluster_vpc(eks_cluster_name)
            if not eks_vpc_id:
                logger.error('Failed to retrieve EKS cluster VPC ID ❌')
                return

            # Check if the VPC IDs are different
            if msk_vpc_id != eks_vpc_id:
                logger.warning(f'MSK and EKS cluster are in different VPCs: {msk_vpc_id} and {eks_vpc_id} 🚧')
            else:
                logger.info(f'MSK and EKS cluster are in the same VPC: {msk_vpc_id}.')

        except Exception as e:
            logger.error(f'Error retrieving MSK/EKS VPC ID {e}')
            return

    def setup_reachability_path_to_db(self, database_name: str, eks_cluster_name: str) -> str | None:
        """
        Setup the reachability path between the database and the EKS cluster
        """
        ec2 = self.session.client('ec2')

        database_ip = self.db_utils.get_database_ip_address(database_name)
        if not database_ip:
            return None

        db_eni = self.db_utils.get_db_eni(database_ip)
        if not db_eni:
            return None

        eks_instance_id = self.get_eks_instance_id(eks_cluster_name)
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
            return None

    def setup_reachability_path_to_msk(self, msk_cluster_arn: str, eks_cluster_name: str) -> str | None:
        """
        Setup the reachability path between the MSK cluster and the EKS cluster
        """
        ec2 = self.session.client('ec2')

        msk_eni = self.msk_utils.get_msk_eni(msk_cluster_arn)
        if not msk_eni:
            return None

        eks_instance_id = self.get_eks_instance_id(eks_cluster_name)
        if not eks_instance_id:
            return None

        try:
            response = ec2.create_network_insights_path(
                Source=eks_instance_id,
                Destination=msk_eni,
                Protocol='TCP',
            )
            path_id = response['NetworkInsightsPath']['NetworkInsightsPathId']
            logger.info(f'Network Insights Path ID: {path_id}')
            return path_id
        except Exception as e:
            logger.error(f'Error setting up reachability path: {e}')
            return None
