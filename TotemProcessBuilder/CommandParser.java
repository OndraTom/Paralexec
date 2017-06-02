package TotemProcessBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Command parser.
 * 
 * @author Ondřej Tom <info@ondratom.cz>
 */
public class CommandParser
{
	/**
	 * Processed command.
	 * 
	 * Changed in parsing process.
	 */
	private String processedCommand;
	
	
	/**
	 * Program calls.
	 */
	private List<String[]> programCalls = new ArrayList<>();
	
	
	/**
	 * Standard output file path.
	 */
	private String standardOutputFilePath = null;
	
	
	/**
	 * Error output file path.
	 */
	private String errorOutputFilePath = null;
	
	
	/**
	 * @param command
	 * @throws CommandParserException 
	 */
	public CommandParser(String command) throws CommandParserException
	{
		this.processedCommand = command;
		
		// Parsing from the end of the command.
		this.parseErrorOutput();
		this.parseStandardOutput();
		this.parseProgramCalls();
	}
	
	
	/**
	 * @return program calls
	 */
	public List<String[]> getProgramCalls()
	{
		return this.programCalls;
	}
	
	
	/**
	 * @return TRUE if the standard output file is set, FALSE otherwise
	 */
	public boolean hasStandardOutputFileSet()
	{
		return this.standardOutputFilePath != null;
	}
	
	
	/**
	 * @return standard output file path
	 */
	public String getStandardOutputFilePath()
	{
		return this.standardOutputFilePath;
	}
	
	
	/**
	 * @return TRUE if the error output file is set, FALSE otherwise
	 */
	public boolean hasErrorOutputFileSet()
	{
		return this.errorOutputFilePath != null;
	}
	
	
	/**
	 * @return error output file path
	 */
	public String getErrorOutputFilePath()
	{
		return this.errorOutputFilePath;
	}
	
	
	/**
	 * Parses error output file path.
	 * 
	 * @throws CommandParserException 
	 */
	private void parseErrorOutput() throws CommandParserException
	{
		String[] split = this.processedCommand.split(" 2\\>");
		
		if (split.length > 2)
		{
			throw new CommandParserException("Multiple definitions of the error output detected.");
		}
		else if (split.length == 2)
		{
			this.processedCommand		= split[0].trim();
			this.errorOutputFilePath	= split[1].trim();
		}
	}
	
	
	/**
	 * Parses standard output file path.
	 * 
	 * @throws CommandParserException 
	 */
	private void parseStandardOutput() throws CommandParserException
	{
		String[] split = this.processedCommand.split(" \\>");
		
		if (split.length > 2)
		{
			throw new CommandParserException("Multiple definitions of the standard output detected.");
		}
		else if (split.length == 2)
		{
			this.processedCommand		= split[0].trim();
			this.standardOutputFilePath	= split[1].trim();
		}
	}
	
	
	/**
	 * Parses program call separated by | (pipe).
	 */
	private void parseProgramCalls()
	{
		int i;
		String[] split = this.processedCommand.split("\\|");
		
		for (String programCall : split)
		{
			String[] programCallParts = programCall.trim().split(" ");
			
			for (i = 0; i < programCallParts.length; i++)
			{
				programCallParts[i] = programCallParts[i].trim();
			}
			
			this.programCalls.add(programCallParts);
		}
	}
}
