package paralexec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.json.*;

/**
 * Main class for parallel executing of the Totem processes scripts.
 *
 * @author oto
 */
public class Paralexec implements ExecListener
{
	/**
	 * Number of running threads.
	 */
	protected int runningThreads = 0;


	/**
	 * Path to the running flag file.
	 */
	protected Path runningFlagFilePath;


	public Paralexec()
	{
		this.runningFlagFilePath = Paths.get(this.getCurrentDir() + File.separator + "running");
	}


	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		Paralexec paralexec = new Paralexec();

		try
		{
			paralexec.checkArgs(args);

			String jsonString	= paralexec.readFile(args[0]);
			JSONObject json		= new JSONObject(jsonString);
			JSONObject scenario = json.getJSONObject("scenario");

			paralexec.processSettings(scenario, args[1]);
		}
		catch (Exception e)
		{
			Paralexec.handleError(e);
		}
	}


	/**
	 * @return Current dir location.
	 */
	protected String getCurrentDir()
	{
		File currentDir = new File(".");

		return currentDir.getAbsolutePath();
	}


	/**
	 * Checks the CLI arguments.
	 *
	 * Exit the application in case of args invalidity.
	 *
	 * @param args
	 */
	protected void checkArgs(String[] args)
	{
		if (args.length < 1)
		{
			System.out.println("Execution scenario not provided.");
			System.exit(0);
		}

		if (args.length < 2)
		{
			System.out.println("Processes scripts directory not provided.");
			System.exit(0);
		}
	}


	/**
	 * Process the zero level settings.
	 *
	 * @param settings
	 * @param scriptsDir
	 */
	protected void processSettings(JSONObject settings, String scriptsDir)
	{
		Iterator keys = settings.keys();

		while (keys.hasNext())
		{
			String settingId = (String) keys.next();

			try
			{
				Exec exec = new Exec(settings.getJSONObject(settingId), scriptsDir, this);

				exec.start();
			}
			catch (JSONException e)
			{
				this.handleError(e);
			}
		}
	}


	/**
	 * Returns the content of the json execution scenario file.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected String readFile(String path) throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));

		return new String(encoded);
	}


	@Override
	public void manageThreadStart()
	{
		this.runningThreads++;

		if (!Files.exists(this.runningFlagFilePath))
		{
			try
			{
				Files.createFile(this.runningFlagFilePath);
			}
			catch (IOException e)
			{
				this.handleError(e);
			}
		}
		else
		{
			System.out.println("The running file exists.");
		}
	}


	@Override
	public void manageThreadEnd()
	{
		this.runningThreads--;

		if (this.runningThreads <= 0 && Files.exists(this.runningFlagFilePath))
		{
			try
			{
				Files.delete(this.runningFlagFilePath);
			}
			catch (IOException e)
			{
				this.handleError(e);
			}
		}
	}


	/**
	 * Handles the exception.
	 *
	 * @param e
	 */
	protected static void handleError(Exception e)
	{
		Paralexec.writeErrorMessage(e.getMessage());
	}


	/**
	 * Write error message on output.
	 *
	 * @param msg
	 */
	protected static void writeErrorMessage(String msg)
	{
		System.out.println("Exec error: " + msg);
	}
}