package Database.Drivers;

import Database.DatabaseException;

/**
 *
 * @author ODIS
 */
final public class DbDriverException extends DatabaseException
{
	public DbDriverException()
	{
		super();
	}
	
	
	public DbDriverException(String message)
	{
		super(message);
	}
	
	
	public DbDriverException(String message, Throwable cause)
	{
		super(message, cause);
	}
}