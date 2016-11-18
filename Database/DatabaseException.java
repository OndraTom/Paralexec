package Database;

/**
 * Exception associated with DB processing.
 * 
 * @author ODIS
 */
public class DatabaseException extends Exception
{
	public DatabaseException()
	{
		super();
	}
	
	
	public DatabaseException(String message)
	{
		super(message);
	}
	
	
	public DatabaseException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
