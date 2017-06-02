package TotemProcessBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Command validator.
 * 
 * @author Ondřej Tom <info@ondratom.cz>
 */
public class CommandValidator
{
	/**
	 * Command parser.
	 */
	private CommandParser parser;
	
	
	/**
	 * Invalidity reasons.
	 */
	private List<String> invalidityReasons = new ArrayList<>();
	
	
	/**
	 * @param parser 
	 */
	public CommandValidator(CommandParser parser)
	{
		this.parser = parser;
		
		this.validateProgramCalls();
	}
	
	
	/**
	 * @return TRUE if command is valid, FALSE otherwise
	 */
	public boolean isValid()
	{
		return invalidityReasons.isEmpty();
	}
	
	
	/**
	 * @return invalidity reasons
	 */
	public List<String> getInvalidityReasons()
	{
		return this.invalidityReasons;
	}
	
	
	/**
	 * Validates program calls in command.
	 */
	private void validateProgramCalls()
	{
		if (parser.getProgramCalls().isEmpty())
		{
			this.invalidityReasons.add("No program calls.");
		}
	}
}
