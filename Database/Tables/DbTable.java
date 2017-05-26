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
}
