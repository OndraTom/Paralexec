package Database.Tables;

import Database.Drivers.DbDriver;
import Database.Drivers.DbDriverException;
import Database.DatabaseException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author ODIS
 */
abstract public class DbTable
{
	abstract protected DbDriver getDb() throws DbDriverException;
	
	
	abstract protected String getTableName();
	
	
	protected Connection getDbConnection() throws DbDriverException
	{
		return this.getDb().getConnection();
	}
	
	
	protected String getSelectAllSql()
	{
		return "SELECT * FROM " + this.getTableName();
	}
	
	
	public ResultSet query(String sql) throws DatabaseException
	{
		try
		{
			Statement stmt = this.getDbConnection().createStatement();
		
			return stmt.executeQuery(sql);
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public int updateQuery(String sql) throws DatabaseException
	{
		try
		{
			Statement stmt = this.getDbConnection().createStatement();
			
			return stmt.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public ResultSet getAll() throws DatabaseException
	{
		return this.query(this.getSelectAllSql());
	}
}
