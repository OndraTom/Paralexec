package Database.Drivers;

/**
 *
 * @author ODIS
 */
final public class TotemDbDriver extends DbDriver
{
	public TotemDbDriver() throws DbDriverException
	{
		super();
	}
	
	
	@Override
	final protected String getDb()
	{
		return "jdbc:mysql://localhost/totem";
	}
	
	
	@Override
	final protected String getUser()
	{
		return "root";
	}
	
	
	@Override
	final protected String getPassword()
	{
		return "root";
	}
}