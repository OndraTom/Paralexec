package Database.Drivers;

import Config.TotemDb;

/**
 *
 * @author oto
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
		return TotemDb.DB;
	}
	
	
	@Override
	final protected String getUser()
	{
		return TotemDb.USER;
	}
	
	
	@Override
	final protected String getPassword()
	{
		return TotemDb.PASSWORD;
	}
}