
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class MonitorStatus
{
	public String host = "";
	public String checktype = "";
	public String retryrate = "";
	public String successrate = "";
	public String statusstring = "Failure";
	public boolean pass = false;
	public String lastfailure = "Unknown";
	public String lastupdate = "";
	public String label = "";
	public boolean reloading = false;
	public boolean enabled = false;
	
	public void passstamp()
	{
		pass = true;
		statusstring = "";
		lastupdate = getDate();
	}
	
	
	public void failstamp(String failuremessage)
	{
		lastfailure = getDate();
		pass = false;
		statusstring = failuremessage;
		lastupdate = getDate();
	}
	
	public static String getDate()
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
        java.util.Date date = new java.util.Date();
        return dateFormat.format(date);
	}
}
