package Database.Tables;

import Database.Drivers.DbDriver;
import Database.Drivers.DbDriverException;
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
	protected Connection dbConnection;
	
	
	public DbTable() throws DbDriverException
	{
		this.dbConnection = this.getDb().getConnection();
	}
	
	
	protected String getSelectAllSql()
	{
		return "SELECT * FROM " + this.getTableName();
	}
	
	
	public ResultSet query(String sql) throws DbTableException
	{
		try
		{
			Statement stmt = this.dbConnection.createStatement();
		
			return stmt.executeQuery(sql);
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public int updateQuery(String sql) throws DbTableException
	{
		try
		{
			Statement stmt = this.dbConnection.createStatement();
			
			return stmt.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			throw new DbTableException(e.getMessage(), e);
		}
	}
	
	
	public ResultSet getAll() throws DbTableException
	{
		return this.query(this.getSelectAllSql());
	}
	
	
	abstract protected DbDriver getDb() throws DbDriverException;
	
	
	abstract protected String getTableName();
}
