package Process;

import Database.Tables.ExecutedProcessesTable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author ODIS
 */
final public class ProcessSettingTree
{
	private ExecutedProcessesTable processTable;


	private final Map<Integer, ProcessSetting> items = new HashMap<>();


	public ProcessSettingTree(ExecutedProcessesTable processTable) throws ProcessSettingException
	{
		this.processTable = processTable;

		this.loadItems();
	}


	private void loadItems() throws ProcessSettingException
	{
		try
		{
                        for (ProcessSetting processSetting : processTable.getWaitingProcesses())
                        {
                                this.addItem(processSetting);
                        }
		}
		catch (Exception e)
		{
			throw new ProcessSettingException("Error while loading tree: " + e.getMessage());
		}
	}


	private void addItem(ProcessSetting item) throws ProcessSettingException
	{
		if (this.items.containsKey(item.getId()))
		{
			throw new ProcessSettingException("Duplicite key " + item.getId());
		}

		this.items.put(item.getId(), item);

		if (!item.isRoot())
		{
			if (this.items.containsKey(item.getParentId()))
			{
				this.items.get(item.getParentId()).addChild(item);
			}
			else
			{
				item.setIsRoot(true);
			}
		}
	}


	public List<ProcessSetting> getRootItems()
	{
		List<ProcessSetting> tree = new ArrayList<>();

		for (Entry<Integer, ProcessSetting> item : this.items.entrySet())
		{
			if (item.getValue().isRoot())
			{
				tree.add(item.getValue());
			}
		}

		return tree;
	}
}
