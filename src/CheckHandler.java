
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public class CheckHandler extends IoHandlerAdapter
{
	private Log log = null;
	private MonitorStatus status = null;
	private HeartbeatMonitorThread monitor = null;
	
	public CheckHandler(Log setLog,MonitorStatus setStatus,HeartbeatMonitorThread setMonitor)
	{
		super();
		log = setLog;
		status = setStatus;
		monitor = setMonitor;
	}
	
	public void messageReceived(IoSession session, Object message)
	{
		//System.out.println("Message Received: "+message.toString());
		if (message.toString().equals("goingtoreload"))
		{
			//System.out.println("Server is having a planned reload, ignore disconnect and connect errors");
			log.logit("Heartbeat connection having planned disconnect due to server reload: "+session.getRemoteAddress(),"",status);
			status.reloading = true;
		}
	}
	
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		if (!status.reloading)
		{
			//System.out.println("Check Handler Exception Caught");
			log.logit("Problem with heartbeat connection to "+session.getRemoteAddress(),"failure",status);
			status.failstamp("Problem with heartbeat connection");
			monitor.alert("Problem with heartbeat connection to "+session.getRemoteAddress());
			//cause.printStackTrace();
			monitor.lastcheck = false;
		}
		else
		{
			log.logit("Expected heartbeat connection exception due to server reload - "+session.getRemoteAddress(),"",status);
		}
		session.close();
	}
	
	public void sessionClosed(IoSession session) throws Exception
	{
		if (!status.reloading)
		{
			//System.out.println("Session closed detected");
			monitor.lastcheck = false;
			log.logit("Heartbeat Session to "+session.getRemoteAddress()+" closed","failure",status);
			status.failstamp("Heartbeat Session Closed");
			monitor.alert("Heartbeat Session to "+session.getRemoteAddress()+" closed");
		}
		else
		{
			log.logit("Expected Heartbeat Session loss due to server reload: "+session.getRemoteAddress(),"failure",status);
			status.reloading = false;
			Thread.sleep(5000);
		}
	}
}
