import boto3
from checks.utils.set_logging import logger

class DatabaseChecks:
    def __init__(self, session: boto3.Session, database_name: str):
        self.session = session
        self.rds = self.session.client('rds')
        self.database_name = database_name

    def check_database(self) -> None:
        """
        Check if the database is available
        """
        database_name = self.database_name
        logger.info(f'Checking database {database_name}...')
        try:
            response = self.rds.describe_db_instances(DBInstanceIdentifier=database_name)
            engine = response['DBInstances'][0]['Engine']
            db_instance_status = response['DBInstances'][0]['DBInstanceStatus']
            if engine in ['postgres', 'aurora-postgresql'] and db_instance_status == 'available':
                logger.info(f'Database {database_name} is available ✅')
            else:
                logger.error(f'Database {database_name} is not available or is not of type postgres/aurora-postgresql ❌')
        except Exception as e:
            logger.error(f'Unable to check if database exists and is available: {e}')

    def check_database_parameter_group(self) -> None:
        """
        Check if the database parameter group is available and is attached to the database
        """
        database_name = self.database_name
        logger.info(f'Checking database parameter group for {database_name}...')
        try:
            response = self.rds.describe_db_instances(DBInstanceIdentifier=database_name)
            parameter_group_name = response['DBInstances'][0]['DBParameterGroups'][0]['DBParameterGroupName']
            if parameter_group_name:
                logger.info(f'Database parameter group {parameter_group_name} is attached to the database ✅')
            else:
                logger.error('Database parameter group is not attached to the database ❌')
        except Exception as e:
            logger.error(f'Unable to check if the parameter group is available/attached to database: {e}')

    def check_logical_replication(self) -> None:
        """
        Check if the database parameter group has rds.logical_replication enabled
        """
        database_name = self.database_name
        logger.info(f'Checking logical replication for {database_name}...')
        try:
            response = self.rds.describe_db_instances(DBInstanceIdentifier=database_name)
            parameter_group_name = response['DBInstances'][0]['DBParameterGroups'][0]['DBParameterGroupName']

            all_parameters = []
            marker = None

            # loop to handle pagination
            while True:
                if marker:
                    response = self.rds.describe_db_parameters(DBParameterGroupName=parameter_group_name, Marker=marker)
                else:
                    response = self.rds.describe_db_parameters(DBParameterGroupName=parameter_group_name)
                all_parameters.extend(response['Parameters'])
                if 'Marker' in response:
                    marker = response['Marker']
                else:
                    break

            for param in all_parameters:
                if param['ParameterName'] == 'rds.logical_replication':
                    if param['ParameterValue'] == '1':
                        logger.info(f'Logical replication is enabled in {parameter_group_name} ✅')
                    else:
                        logger.error(f'Logical replication is not enabled in {parameter_group_name} ❌')
                    return
        except Exception as e:
            logger.error(f'Unable to check if logical replication is enabled for the parameter group: {e}')

    def check_logical_replication_effect(self) -> None:
        """
        Check if logical replication has taken effect
        """
        database_name = self.database_name
        logger.info(f'Checking if logical replication has taken effect in {database_name}...')
        try:
            response = self.rds.describe_db_instances(DBInstanceIdentifier=database_name)
            parameter_apply_status = response['DBInstances'][0]['DBParameterGroups'][0]['ParameterApplyStatus']
            if parameter_apply_status == 'in-sync':
                logger.info(f'No need for reboot as there no pending-reboot updates in {database_name} ✅')
            elif parameter_apply_status == 'pending-reboot':
                logger.error(f'Reboot required to make logical replication changes effective in {database_name}, please reboot ❌')
            else:
                logger.error(f'Logical replication has not taken effect in {database_name} ❌')
        except Exception as e:
            logger.error(f'Unable to check if the logical replication has taken effect in the parameter group: {e}')

    def perform_all_database_checks(self) -> None:
        """
        Perform all database checks
        """
        self.check_database()
        self.check_database_parameter_group()
        self.check_logical_replication()
        self.check_logical_replication_effect()