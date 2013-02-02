
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
//import org.apache.mina.filter.logging.*;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.*;
import org.apache.mina.core.session.IoSession;

public class PathMonitor
{
	public static void main(String[] args)
	{
		String specloc = System.getProperty("spec");
		if (specloc == null)
		{
			System.out.print("No spec file given, quitting");
			System.exit(0);
		}
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
        java.util.Date date = new java.util.Date();
        String startstamp = dateFormat.format(date);
        
        boolean firstrun = true;

		while (true)
		{
			System.out.println("Loading XML Spec file: "+specloc);
			//BufferedWriter out = null;
			SocketAcceptor acceptor = null;
			SocketAcceptor statusacceptor = null;

			Vector<MonitorThread> monitorlist = new Vector<MonitorThread>();
			try
			{
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	            Document doc = docBuilder.parse (new File(specloc));
	            
	            doc.getDocumentElement().normalize();
	            String heartbeatportString = doc.getDocumentElement().getAttribute("heartbeatport");
	            String statusportString = doc.getDocumentElement().getAttribute("statusport");
	            String logfiledir = doc.getDocumentElement().getAttribute("logfiledir");
	            String masterlabel = doc.getDocumentElement().getAttribute("label");
	            String nodeid = doc.getDocumentElement().getAttribute("id");
	            String smtphost = doc.getDocumentElement().getAttribute("smtphost");
	            String l1mailto = doc.getDocumentElement().getAttribute("l1mailto");
	            String l2mailto = doc.getDocumentElement().getAttribute("l2mailto");
	            String l2threshold = doc.getDocumentElement().getAttribute("l2threshold");
	            
	            if (heartbeatportString == null || heartbeatportString.equals(""))
	            {
	            	System.out.println("No heartbeat specified in spec, quitting");
	            	System.exit(0);
	            }
	            if (statusportString == null || statusportString.equals(""))
	            {
	            	System.out.println("No status port specified in spec, quitting");
	            	System.exit(0);
	            }
	            if (logfiledir == null || logfiledir.equals(""))
	            {
	            	System.out.println("No logfile directory location specified in spec, quitting");
	            	System.exit(0);
	            }
	            
	            int heartbeatport = Integer.valueOf(heartbeatportString).intValue();
	            System.out.println("This Monitor is using port "+heartbeatport+" for heartbeat");
	            
	            int statusport = Integer.valueOf(statusportString).intValue();
	            System.out.println("This Monitor is using port "+statusport+" for status updates");
	            
	            System.out.println("Logging to directory "+logfiledir);
	            Log log = new Log(logfiledir);
	            log.nodelabel = masterlabel;
	            log.setMailer(smtphost, l1mailto, l2mailto, convertStringToInt(l2threshold));

	            if (firstrun)
	            {
	            	firstrun = false;
	            	log.logit("Monitor Node Started","start",null);
	            }
	            log.logit("Logging started","",null);
	            
	            ThreadPing threadping = new ThreadPing(log,false);
	            Thread pinger = new Thread(threadping);
	            threadping.thisThread = pinger;
	            pinger.start();
	            log.pinger = threadping;
	            
	            //ShutdownHook hook = new ShutdownHook(out);
	            //Runtime.getRuntime().addShutdownHook(hook);
	            
	            // Starting Heartbeat server
	            
	            acceptor = new NioSocketAcceptor();
	            acceptor.getSessionConfig().setReuseAddress(true);
	            acceptor.getSessionConfig().setTcpNoDelay(true);
	            acceptor.setReuseAddress(true);
	            //acceptor.getFilterChain().addLast("logger", new LoggingFilter());
	            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
	            acceptor.setReuseAddress(true);
	            //acceptor.setCloseOnDeactivation(true);
	            acceptor.getSessionConfig().setTcpNoDelay(true);
	            
	            acceptor.setDefaultLocalAddress(new InetSocketAddress(heartbeatport));
	            Vector<IoSession> sessionlist = new Vector<IoSession>();
	            acceptor.setHandler(new HeartBeatHandler(sessionlist));
	            acceptor.bind();
	            
	            
	            // Starting Path Monitoring
	            NodeList listOfServers = doc.getElementsByTagName("check");
	            int totalServers = listOfServers.getLength();
	            System.out.println("Total # of checks: " + totalServers);
	            
	            //Vector<PingMonitorThread> pinglist = new Vector<PingMonitorThread>();
	            //Vector<PortMonitorThread> portlist = new Vector<PortMonitorThread>();
	            //Vector<HeartbeatMonitorThread> heartbeatlist = new Vector<HeartbeatMonitorThread>();
	            
	            System.out.println();
	            for (int i=0;i < totalServers;i++)
	            {
	            	Node serverNode = listOfServers.item(i);
	            	
	            	Element serverNodeElement = (Element)serverNode;
	            	String checklabel = serverNodeElement.getAttribute("label");
	            	String host = serverNodeElement.getElementsByTagName("host").item(0).getChildNodes().item(0).getNodeValue();
	            	String checktype = serverNodeElement.getElementsByTagName("checktype").item(0).getChildNodes().item(0).getNodeValue();
	            	boolean enabled = true;
	            	if (serverNodeElement.getElementsByTagName("enabled").item(0) != null && serverNodeElement.getElementsByTagName("enabled").item(0).getChildNodes().item(0).getNodeValue().toLowerCase().equals("false"))
	            	{
	            		enabled = false;
	            	}
	            	String successrateString = serverNodeElement.getElementsByTagName("successrate").item(0).getChildNodes().item(0).getNodeValue();
	            	String retryrateString = serverNodeElement.getElementsByTagName("retryrate").item(0).getChildNodes().item(0).getNodeValue();
	            	//String l2thresholdoverride = serverNodeElement.getElementsByTagName("l2threshold").item(0).getChildNodes().item(0).getNodeValue();
	            	String l2thresholdoverride = getElementValue("l2threshold",serverNodeElement);
	            	//String l1mailtoadd = serverNodeElement.getElementsByTagName("l1mailto").item(0).getChildNodes().item(0).getNodeValue();
	            	String l1mailtoadd = getElementValue("l1mailto",serverNodeElement);
	            	//String l2mailtoadd = serverNodeElement.getElementsByTagName("l2mailto").item(0).getChildNodes().item(0).getNodeValue();
	            	String l2mailtoadd = getElementValue("l2mailto",serverNodeElement);
	            	
	            	int l2thresholdoverrideint = convertStringToInt(l2thresholdoverride);
	            	String[] l1mailtoaddarray = new String[0];
	            	String[] l2mailtoaddarray = new String[0];
	            	if (!l1mailtoadd.equals(""))
	            	{
	            		l1mailtoaddarray = l1mailtoadd.split(",");
	            	}
	            	if (!l2mailtoadd.equals(""))
	            	{
	            		l2mailtoaddarray = l2mailtoadd.split(",");
	            	}
	            	
	            	//System.out.println(l1mailtoaddarray.length + "-" + l2mailtoaddarray.length);
	            	
	            	int successrate = 30;
	            	int retryrate = 30;
	            	if (host == null || host.equals(""))
	            	{
	            		System.out.println("No Host, skipping this path");
	            		enabled = false;
	            	}
	            	
	            	if (successrateString == null || successrateString.equals(""))
	            	{
	            		System.out.println("No Success Rate defined, using default ("+successrate+" seconds)");
	            	}
	            	else
	            	{
	            		successrate = Integer.valueOf(successrateString).intValue();
	            	}
	            	
	            	if (retryrateString == null || retryrateString.equals(""))
	            	{
	            		System.out.println("No retry rate defined, using default ("+retryrate+" seconds)");
	            	}
	            	else
	            	{
	            		retryrate = Integer.valueOf(retryrateString).intValue();
	            	}
	            	
	            	System.out.println(checklabel);
	            	
	            	if (checktype.equals("heartbeat"))
	            	{
		            	String portString = serverNodeElement.getElementsByTagName("port").item(0).getChildNodes().item(0).getNodeValue();
		
		            	if (portString == null || portString.equals(""))
		            	{
		            		System.out.println("No Heartbeat, skipping this path");
		            	}
		            	else
		            	{
			            	int heartbeat = Integer.valueOf(portString).intValue();
			            	System.out.println("Starting Heartbeat Check:");
			            	System.out.println("Host: "+host);
			            	System.out.println("Port: "+heartbeat);
			            	System.out.println();
			            	HeartbeatMonitorThread m = new HeartbeatMonitorThread(host,heartbeat,log,successrate,retryrate,checklabel);
			            	m.setAlertOverride(l2thresholdoverrideint, l1mailtoaddarray, l2mailtoaddarray);
			            	Thread t = new Thread(m);
			            	m.thisthread = t;
			            	if (enabled)
			            	{
			            		t.start();
			            		log.logit("Starting Heartbeat Check - "+host+":"+heartbeat,"enable-"+successrate+"-"+retryrate,m.getStatus());
			            	}
			            	else
			            	{
			            		log.logit("Heartbeat Check - "+host+":"+heartbeat+" Disabled on start","disable",m.getStatus());
			            	}
			            	monitorlist.add(m);
			            	
		            	}
	            	}
	            	else if (checktype.equals("ping"))
	            	{
		            	System.out.println("Starting Ping Check:");
		            	System.out.println("Host: "+host);
		            	System.out.println();
		            	PingMonitorThread m = new PingMonitorThread(host,log,successrate,retryrate,checklabel);
		            	m.setAlertOverride(l2thresholdoverrideint, l1mailtoaddarray, l2mailtoaddarray);
		            	Thread t = new Thread(m);
		            	m.thisthread = t;
		            	if (enabled)
		            	{
		            		t.start();
		            		log.logit("Starting Ping Check - "+host,"enable-"+successrate+"-"+retryrate,m.getStatus());
		            	}
		            	else
		            	{
		            		log.logit("Ping Check - "+host+" Disabled on start","disable",m.getStatus());
		            	}
		            	monitorlist.add(m);
	            	}
	            	else if (checktype.equals("port"))
	            	{
		            	String portString = serverNodeElement.getElementsByTagName("port").item(0).getChildNodes().item(0).getNodeValue();
		            	
		            	if (portString == null || portString.equals(""))
		            	{
		            		System.out.println("No Port Given, skipping");
		            	}
		            	else
		            	{
			            	int heartbeat = Integer.valueOf(portString).intValue();
			            	System.out.println("Starting Port Check:");
			            	System.out.println("Host: "+host);
			            	System.out.println("Port: "+heartbeat);
			            	System.out.println();
			            	
			            	PortMonitorThread m = new PortMonitorThread(host,heartbeat,log,successrate,retryrate,checklabel);
			            	m.setAlertOverride(l2thresholdoverrideint, l1mailtoaddarray, l2mailtoaddarray);
			            	Thread t = new Thread(m);
			            	m.thisthread = t;
			            	if (enabled)
			            	{
			            		t.start();
			            		log.logit("Starting Port Check - "+host+":"+heartbeat,"enable-"+successrate+"-"+retryrate,m.getStatus());
			            	}
			            	else
			            	{
			            		log.logit("Port Check - "+host+":"+heartbeat+" Disabled on start","disable",m.getStatus());
			            	}
			            	monitorlist.add(m);
		            	}            		
	            	}
	            }
	            
	            statusacceptor = new NioSocketAcceptor();
	            statusacceptor.setReuseAddress(true);
	            //statusacceptor.getFilterChain().addLast("logger", new LoggingFilter());
	            statusacceptor.getSessionConfig().setTcpNoDelay(true);
	            statusacceptor.getSessionConfig().setKeepAlive(true);
	            statusacceptor.getSessionConfig().setBothIdleTime(5);
	            statusacceptor.getSessionConfig().setReaderIdleTime(5);
	            statusacceptor.getSessionConfig().setWriteTimeout(5);
	            //statusacceptor.setReuseAddress(true);
	            statusacceptor.setCloseOnDeactivation(true);
	            statusacceptor.setDefaultLocalAddress(new InetSocketAddress(statusport));
	            statusacceptor.setHandler(new StatusHttpProtocolHandler(monitorlist,acceptor,statusacceptor,log,masterlabel,nodeid,sessionlist,startstamp));
	            statusacceptor.bind();
	            
	            System.out.println("Status Listener activated...");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				try
				{
					//out.flush();
					//out.close();
				}
				catch (Exception e2)
				{
				}
				
				try
				{	Thread.sleep(30000);
				}
				catch (Exception e3)
				{
				}
			}
			
			
			for (int x=0;x<monitorlist.size();x++)
			{
				if (monitorlist.get(x).getThisThread() != null)
				{
					try
					{	monitorlist.get(x).getThisThread().join();
					}
					catch (Exception e)
					{
					}
				}
			}
			
			if (acceptor != null)
			{
				while (acceptor.isActive() || !acceptor.isDisposed())
				{
					try
					{	Thread.sleep(1000);
					}
					catch (Exception e) {}
				}
			}

			if (statusacceptor != null)
			{
				while (statusacceptor.isActive() || !statusacceptor.isDisposed())
				{
					try
					{	Thread.sleep(1000);
					}
					catch (Exception e) {}
				}
			}
			System.out.println("Main thread has reached end, reloading...");
			/*
			try
			{
				Thread.sleep(9999999);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			*/
		}
	}


	
	public static void processSingle(String data,Map<String,String> variables) throws Exception
	{
		if (data != null && !data.equals(""))
		{
			String[] items = data.split("&");
			for (int x=0;x<items.length;x++)
			{
				int splitter = items[x].indexOf('=');
				String variable = items[x].substring(0,splitter);
				String value = items[x].substring(splitter+1);
				variables.put(variable, URLDecoder.decode(value,"UTF-8"));
			}
		}
	}
	
	public static void processMulti(String data,String boundary,Map<String,String> variables)
	{
		String[] results = data.split(boundary);

		if (results.length > 1)
		{
			for (int x=1;x<results.length-1;x++)
			{
				String item = results[x].substring(0, results[x].length()-2).trim();
				
				int c = item.indexOf("name=") + 6;
				String variable = "";
				int count = 0;
				while (item.charAt(c) != '"' && item.charAt(c) != '\n' && count < 10)
				{
					variable += (char)item.charAt(c);
					c++;
					count++;
				}
				
				c = item.indexOf("\n\n") + 2;
				String value = item.substring(c);
				variables.put(variable, value);
			}
		}
	}	
	
	private static int convertStringToInt(String number)
	{
		int toreturn = -1;
		
		try
		{
			toreturn = Integer.valueOf(number).intValue();
		}
		catch (Exception e)
		{
		}
		
		//System.out.println("Converted "+number+" to "+toreturn);
		
		return toreturn;
	}
	
	private static String getElementValue(String elementName,Element baseElement)
	{
		String toreturn = "";
		if (baseElement.getElementsByTagName(elementName).item(0) != null)
		{
			toreturn = baseElement.getElementsByTagName(elementName).item(0).getChildNodes().item(0).getNodeValue();
		}
		//System.out.println(elementName+": "+toreturn);
		return toreturn;
		//String l2thresholdoverride = 
	}
}
