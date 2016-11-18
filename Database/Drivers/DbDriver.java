package Database.Drivers;

import java.sql.*;

/**
 *
 * @author ODIS
 */
abstract public class DbDriver
{
	final private static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	
	
	private Connection connection;
	
	
	public DbDriver() throws DbDriverException
	{
		this.loadConnection();
	}
	
	
	private void loadConnection() throws DbDriverException
	{
		try
		{
			Class.forName(JDBC_DRIVER);
			
			this.connection = DriverManager.getConnection(
					this.getDb(), 
					this.getUser(), 
					this.getPassword()
			);
		}
		catch (Exception e)
		{
			throw new DbDriverException("Database connection error: " + e.getMessage());
		}
	}
	
	
	public Connection getConnection()
	{
		return this.connection;
	}
	
	
	abstract protected String getDb();
	
	
	abstract protected String getUser();
	
	
	abstract protected String getPassword();
}