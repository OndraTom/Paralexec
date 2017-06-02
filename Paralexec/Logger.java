package Paralexec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Paralexec events logger.
 *
 * @author oto
 */
final public class Logger
{
	/**
	 * Log file path.
	 */
	private static String logFile = null;


	/**
	 * Log file writer.
	 */
	private static PrintWriter writer = null;


	/**
	 * @return Log file path.
	 */
	private static String getLogFile()
	{
		if (logFile == null)
		{
			File currentDir = new File(".");
			
			logFile = currentDir.getAbsolutePath() + File.separator + "log_" + getTime();
		}

		return logFile;
	}


	/**
	 * @return	Log file writer.
	 * @throws	IOException
	 */
	private static PrintWriter getWriter() throws IOException
	{
		if (writer == null)
		{
			FileWriter fw		= new FileWriter(getLogFile(), true);
			BufferedWriter bw	= new BufferedWriter(fw);
			writer				= new PrintWriter(bw);
		}

		return writer;
	}


	/**
	 * @return Current time string.
	 */
	private static String getTime()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

		return dateFormat.format(new Date());
	}


	/**
	 * Writes message to the log file.
	 *
	 * @param Message
	 */
	private static void writeMessage(String message)
	{
		try
		{
			PrintWriter fileWriter = getWriter();

			fileWriter.println(message);
			fileWriter.flush();
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
	}


	/**
	 * Logs given message.
	 *
	 * @param message
	 */
	public static void log(String message)
	{
		writeMessage(getTime() + " - " + message);
	}


	/**
	 * Logs given error message.
	 *
	 * @param message
	 */
	public static void logError(String message)
	{
		writeMessage(getTime() + " - [error]: " + message);
	}
}