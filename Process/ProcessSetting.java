package Process;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ODIS
 */
final public class ProcessSetting
{
	private int id;
	
	
	private int parentId;
	
	
	private String state;
	
	
	private String scriptPath;
	
	
	private String outputDirPath;
	
	
	private String outputExt;
	
	
	private String error;
	
	
	private List<ProcessSetting> children;
	
	
	private boolean isRoot;
	
	
	public ProcessSetting(
			int		id, 
			int		parentId, 
			String	state, 
			String	scriptPath, 
			String	outputDirPath, 
			String	outputExt,
			String	error
	)
	{
		this.id				= id;
		this.parentId		= parentId;
		this.state			= state;
		this.scriptPath		= scriptPath;
		this.outputDirPath	= outputDirPath;
		this.outputExt		= outputExt;
		this.error			= error;
		this.children		= new ArrayList<>();
		this.isRoot			= this.parentId == 0;
	}
	
	
	public int getId()
	{
		return this.id;
	}
	
	
	public int getParentId()
	{
		return this.parentId;
	}
	
	
	public boolean isRoot()
	{
		return this.isRoot;
	}
	
	
	public void setIsRoot(boolean isRoot)
	{
		this.isRoot = isRoot;
	}
	
	
	public List<ProcessSetting> getChildren()
	{
		return this.children;
	}
	
	
	public void addChild(ProcessSetting child)
	{
		this.children.add(child);
	}
	
	
	public String getScriptPath()
	{
		return this.scriptPath;
	}
	
	
	public boolean hasError()
	{
		return !this.error.equals("");
	}
}
