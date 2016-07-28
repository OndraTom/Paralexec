package paralexec;

import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Thread of the parallel execution.
 *
 * @author oto
 */
public class Exec implements Runnable
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
	 * Exec manager.
	 */
	ExecManager manager;


	/**
	 * Constructor.
	 *
	 * @param setting
	 * @param scriptsDir
	 * @param manager
	 */
	public Exec(JSONObject setting, String scriptsDir, ExecManager manager)
	{
		this.setting	= setting;
		this.scriptsDir = scriptsDir;
		this.manager	= manager;
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
			this.manager.manageExecEnd();
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
				this.manager.manageExecStart(new Exec(children.getJSONObject(settingId), this.scriptsDir, this.manager));
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
}