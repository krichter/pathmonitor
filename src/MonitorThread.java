

public interface MonitorThread
{
	public void stopmonitor();
	public Thread getThisThread();
	public MonitorStatus getStatus();
	public void disablemonitor();
	public void regenerate();
	public void setAlertOverride(int l2threshold,String[] l1mailto,String[] l2mailto);
}
