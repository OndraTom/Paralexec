package Database.Tables;

import Database.DatabaseException;

/**
 *
 * @author ODIS
 */
public class DbTableException extends DatabaseException
{
	public DbTableException()
	{
		super();
	}
	
	
	public DbTableException(String message)
	{
		super(message);
	}
	
	
	public DbTableException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
