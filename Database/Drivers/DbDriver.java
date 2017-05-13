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
		this.connection = this.getNewConnection();
	}
	
	
	private Connection getNewConnection() throws DbDriverException
	{
		try
		{
			Class.forName(JDBC_DRIVER);
			
			return DriverManager.getConnection(
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
	
	
	public Connection getConnection() throws DbDriverException
	{
		try
		{
			if (this.connection.isClosed())
			{
				this.connection = this.getNewConnection();
			}
		}
		catch (SQLException e)
		{
			throw new DbDriverException("Database access error: " + e.getMessage());
		}
		
		return this.connection;
	}
	
	
	abstract protected String getDb();
	
	
	abstract protected String getUser();
	
	
	abstract protected String getPassword();
}