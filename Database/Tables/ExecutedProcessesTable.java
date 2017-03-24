package Database.Tables;

import Database.Drivers.DbDriver;
import Database.Drivers.DbDriverException;
import Database.Drivers.TotemDbDriver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author ODIS
 */
final public class ExecutedProcessesTable extends DbTable
{
	public ExecutedProcessesTable() throws DbDriverException
	{
		super();
	}
	

	@Override
	protected DbDriver getDb() throws DbDriverException
	{
		return new TotemDbDriver();
	}
	
	
	@Override
	protected String getTableName()
	{
		return "executed_processes";
	}
	
	
	public ResultSet getWaitingProcesses() throws DbTableException
	{
		return this.query(this.getSelectAllSql() + " WHERE (state = \"WAITING\" OR (state = \"FINISHED\" AND error IS NOT NULL)) ORDER BY parent_id");
	}
	
	
	public void markProcessAsRunning(int processId) throws DbTableException
	{
		if (this.updateQuery("UPDATE " + this.getTableName() + " SET state = \"RUNNING\", start_time = NOW() WHERE process_setting_dataset_id = " + processId) <= 0)
		{
			throw new DbTableException("No affected rows after process marking.");
		}
	}
	
	
	public void markProcessAsFinished(int processId, String error) throws DbTableException
	{
		try
		{	
			PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + this.getTableName() + " SET state = \"FINISHED\", end_time = NOW(), error = ? WHERE process_setting_dataset_id = " + processId);

			stmt.setString(1, error);
			
			if (stmt.executeUpdate() <= 0)
			{
				throw new DbTableException("No affected rows after process marking.");
			}
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public void stopRunningProcesses() throws DbTableException
	{
		this.updateQuery("UPDATE " + this.getTableName() + " SET state = \"WAITING\", start_time = NULL WHERE state = \"RUNNING\"");
	}
}
