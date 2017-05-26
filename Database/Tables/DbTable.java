package Database.Tables;

import Database.Drivers.DbDriver;
import Database.Drivers.DbDriverException;
import java.sql.Connection;

/**
 *
 * @author ODIS
 */
abstract public class DbTable
{
	protected DbDriver db;
	
	
	public DbTable() throws DbDriverException
	{
		this.db = this.getDb();
	}
	
	
	abstract protected DbDriver getDb() throws DbDriverException;
	
	
	abstract protected String getTableName();
	
	
	protected Connection getDbConnection() throws DbDriverException
	{
		return this.db.getConnection();
	}
	
	
	protected String getSelectAllSql()
	{
		return "SELECT * FROM " + this.getTableName();
	}
}
