package paralexec;

import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Threads of the parallel execution.
 *
 * @author oto
 */
public class Exec implements Runnable, ExecListener
{
	/**
	 * Actual process setting.
	 */
	JSONObject setting;


	/**
	 * Processes scripts directory.
	 */
	String scriptsDir;


	/**
	 * Executing script path.
	 */
	String scriptPath;


	/**
	 * Parent - executor of the Exec.
	 */
	ExecListener parent;


	/**
	 * Constructor.
	 *
	 * @param setting
	 * @param scriptsDir
	 * @param parent
	 */
	public Exec(JSONObject setting, String scriptsDir, ExecListener parent)
	{
		this.setting	= setting;
		this.scriptsDir = scriptsDir;
		this.parent		= parent;
	}


	/**
	 * Returns path to the executing script.
	 *
	 * @return
	 * @throws JSONException
	 */
	protected String getScriptPath() throws JSONException
	{
		if (this.scriptPath == null)
		{
			this.scriptPath = this.scriptsDir + this.setting.getString("script");
		}

		return this.scriptPath;
	}


	/**
	 * Executes the script and waits until its finished.
	 *
	 * @throws JSONException
	 */
	protected void executeScript()
	{
		try
		{
			this.manageThreadStart();

			System.out.println("Running script: " + this.getScriptPath());

			// Start the pipeline process and wait until it end.
			Process process = new ProcessBuilder(this.getScriptPath()).start();
			process.waitFor();

			System.out.println("Script " + this.getScriptPath() + " finished.");
		}
		catch (Exception e)
		{
			System.out.println("Script " + this.getScriptPath() + " finished with error: " + e.getMessage());
		}
		finally
		{
			this.manageThreadEnd();
		}
	}


	/**
	 * Process the setting children.
	 *
	 * @param children
	 */
	protected void processChildren(JSONObject children)
	{
		Iterator keys = children.keys();

		while (keys.hasNext())
		{
			String settingId = (String) keys.next();
			try
			{
				Exec exec = new Exec(children.getJSONObject(settingId), this.scriptsDir, this);

				exec.start();
			}
			catch (JSONException e)
			{
				System.out.println("Exec error: " + e.getMessage());
			}
		}
	}


	@Override
	public void run()
	{
		try
		{
			this.executeScript();

			if (!setting.get("children").toString().equals("[]"))
			{
				this.processChildren(setting.getJSONObject("children"));
			}
		}
		catch (Exception e)
		{
			System.out.println("Exec error: " + e.getMessage());
		}
	}


	/**
	 * Creates and starts the thread.
	 */
	public void start()
	{
		Thread t = new Thread(this);

		t.start();
	}


	@Override
	public void manageThreadStart()
	{
		this.parent.manageThreadStart();
	}


	@Override
	public void manageThreadEnd()
	{
		this.parent.manageThreadEnd();
	}
}
