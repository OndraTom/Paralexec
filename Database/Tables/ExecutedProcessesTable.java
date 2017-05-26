package Database.Tables;

import Database.Drivers.DbDriver;
import Database.Drivers.DbDriverException;
import Database.DatabaseException;
import Database.Drivers.TotemDbDriver;
import Process.ProcessSetting;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ODIS
 */
final public class ExecutedProcessesTable extends DbTable
{
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
	
	
	public List<ProcessSetting> getWaitingProcesses() throws DatabaseException
	{
                try
                {
                        PreparedStatement stmt          = this.getDbConnection().prepareStatement(this.getSelectAllSql() + " WHERE (state = \"WAITING\" OR (state = \"FINISHED\" AND error IS NOT NULL)) ORDER BY parent_id");
                        ResultSet rs                    = stmt.executeQuery();
                        List<ProcessSetting> processes  = new ArrayList<>();
                        
                        while (rs.next())
			{
				processes.add(
					new ProcessSetting(
						rs.getInt(1),
						rs.getInt(2),
						rs.getString(3),
						rs.getString(4),
						rs.getString(5),
						rs.getString(6),
						rs.getString(7),
						rs.getString(8),
						rs.getString(9)
					)
				);
			}
                        
                        rs.close();
                        stmt.close();
                        
                        return processes;
                }
                catch (SQLException e)
                {
                        throw new DbTableException(e.getMessage(), e);
                }
	}
	
	
	public void markProcessAsRunning(int processId) throws DatabaseException
	{
                try
                {
                        PreparedStatement stmt = this.getDbConnection().prepareStatement("UPDATE " + this.getTableName() + " SET state = \"RUNNING\", start_time = NOW() WHERE process_setting_dataset_id = " + processId);
                        
                        if (stmt.executeUpdate() <= 0)
                        {
                                throw new DbTableException("No affected rows after process marking.");
                        }
                        
                        stmt.close();
                }
                catch (SQLException e)
                {
                        throw new DbTableException(e.getMessage(), e);
                }
	}
	
	
	public void markProcessAsFinished(int processId, String error) throws DatabaseException
	{
		try
		{	
			PreparedStatement stmt = this.getDbConnection().prepareStatement("UPDATE " + this.getTableName() + " SET state = \"FINISHED\", end_time = NOW(), error = ? WHERE process_setting_dataset_id = " + processId);

			stmt.setString(1, error);
			
			if (stmt.executeUpdate() <= 0)
			{
				throw new DbTableException("No affected rows after process marking.");
			}
                        
                        stmt.close();
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public void stopRunningProcesses() throws DatabaseException
	{
                try
		{	
			PreparedStatement stmt = this.getDbConnection().prepareStatement("UPDATE " + this.getTableName() + " SET state = \"WAITING\", start_time = NULL WHERE state = \"RUNNING\"");

			stmt.executeUpdate();
                        stmt.close();
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
}
