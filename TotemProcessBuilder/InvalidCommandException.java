package TotemProcessBuilder;

import java.util.List;

/**
 * Invalid command exception.
 * 
 * @author Ondřej Tom <info@ondratom.cz>
 */
public class InvalidCommandException extends Exception
{
	/**
	 * Invalidity reasons.
	 */
	private List<String> invalidityReasons;
	
	
	/**
	 * @param invalidityReasons 
	 */
	public InvalidCommandException(List<String> invalidityReasons)
	{
		this.invalidityReasons = invalidityReasons;
	}
	
	
	/**
	 * @return invalidity reasons
	 */
	public List<String> getInvalidityReasons()
	{
		return this.invalidityReasons;
	}
	
	
	@Override
	public String getMessage()
	{	
		return "Invalidity reasons: " + String.join(", ", (String[]) this.invalidityReasons.toArray());
	}
}
