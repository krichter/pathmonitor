
import java.io.*;
import java.util.*;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.stream.StreamIoHandler;
import org.apache.mina.transport.socket.*;

public class StatusHttpProtocolHandler extends StreamIoHandler
{
	public Vector<MonitorThread> monitorList = null;
	public SocketAcceptor heartbeatAcceptor = null;
	public SocketAcceptor statusAcceptor = null;
	public Log log = null;
	public String label = "";
	public String nodeid = "";
	public String startstamp = "";
	public Vector<IoSession> sessions = null;
	
	public StatusHttpProtocolHandler(Vector<MonitorThread> setMonitorList,SocketAcceptor setHeartbeatAcceptor,SocketAcceptor setStatusAcceptor,Log setLog,String setlabel,String setNodeid,Vector<IoSession> setSessions,String setStartstamp)
	{
		monitorList = setMonitorList;
		heartbeatAcceptor = setHeartbeatAcceptor;
		statusAcceptor = setStatusAcceptor;
		log = setLog;
		label = setlabel;
		nodeid = setNodeid;
		sessions = setSessions;
		startstamp = setStartstamp;
	}
	
	public void processStreamIo(IoSession session, InputStream in, OutputStream out)
	{
		Worker w = new Worker(in,out,this,session);
		w.start();
	}

	public static class Worker extends Thread
	{
		private final InputStream in;
		private final OutputStream out;
		private StatusHttpProtocolHandler h;
		private final IoSession session;
		
		public Worker(InputStream in, OutputStream out,StatusHttpProtocolHandler setH,IoSession setSession)
		{
			setDaemon(true);
			this.in = in;
			this.out = out;
			this.h = setH;
			this.session = setSession;
		}
		
		public void run()
		{
			//h.log.logit("Run Start", "test", null);
			String output = "";
			String url;
			BufferedReader in = new BufferedReader(new InputStreamReader(this.in));
			PrintWriter out2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.out)));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(this.out));
			//OutputStreamWriter out2 = new OutputStreamWriter(this.out);
			//OutputStream out = this.out;
			Map<String,String> headers = new TreeMap<String,String>();
			Map<String,String> variables = new TreeMap<String,String>();

			try
			{
				url = in.readLine().split(" ")[1];
				String line;
				while ((line = in.readLine()) != null && !line.equals(""))
				{
					String[] tokens = line.split(": ");
					headers.put(tokens[0], tokens[1]);
				}
				
				if (headers.get("Content-Type") != null)
				{
					String type = (String)headers.get("Content-Type");
					if (type.equals("application/x-www-form-urlencoded"))
					{
						//System.out.println("This is appears to be from a form");
						String data = "";
						while (in.ready())
						{
							data += (char)in.read();
						}
						PathMonitor.processSingle(data,variables);
					}
					else if (type.substring(0, 19).equals("multipart/form-data"))
					{
						//System.out.println("This appears to be a multi-part form submission");
						String boundary = type.substring(type.indexOf('=')+1);
						//System.out.println("Boundary: "+boundary);
						
						String data = "";
						while (in.ready())
						{
							char v = (char)in.read();
							if (v != '\r')
							{
								data += v;
							}
						}
						
						PathMonitor.processMulti(data,boundary,variables);
					}					
				}
				
				int getloc = url.indexOf('?');
				if (getloc > 0)
				{
					//System.out.println("Appears to be a GET query");
					PathMonitor.processSingle(url.substring(getloc+1),variables);
					url = url.substring(0,getloc);
				}				
				
				/*
				if (url.equals("/"))
				{
					out.println("HTTP/1.0 200 OK");
					out.println("Content-Type: text/html");
					out.println("Server: Test Server");
					out.println();
					out.println("<html><head><title>Status</title></head><body>");
					out.println("moo");
					out.println("</body></html>");
				}
				*/
				//h.log.logit("Run Check "+url, "test", null);
				if (url.equals("/log"))
				{
					out2.println("HTTP/1.0 200 OK");
					out2.println("Content-Type: text/html");
					out2.println("Server: Test Server");
					out2.println();
					out2.println("<html><head><title>"+h.label+": Log</title></head><body>");
					out2.println("<STYLE>BODY {font-family:verdana;font-size:10px} TD {font-family:verdana;font-size:10px}</STYLE>");
					out2.println("<H2>Log: "+h.label+"</H2");
					out2.println("<A HREF='/listlogs'>Back to log list</A><BR><BR>");
					out2.println("<PRE>");
					String logfile = h.log.logfiledir + variables.get("logfile");
					h.log.writeLog(out2,logfile);
					out2.println("</PRE>");
					out2.println("</body></html>");				
				}
				else if (url.equals("/rawlog"))
				{
					out2.println("HTTP/1.0 200 OK");
					out2.println("Content-Type: text/plain");
					out2.println("Server: Test Server");
					out2.println();
					String logfile = h.log.logfiledir + variables.get("logfile");
					h.log.writeRawLog(out2,logfile);
				}
				else if (url.equals("/listlogs"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/html\n";
					output += "Server: Test Server\n\n";
					output += "<html><head><title>"+h.label+": List Logs</title></head><body>\n";
					output += "<STYLE>BODY {font-family:verdana;font-size:10px} TD {font-family:verdana;font-size:10px} TH {font-family:verdana;font-size:12px}</STYLE>";
					output += "<H2>Available Logs</H2>";
					output += "<A HREF='/'>Back to home</A><BR><BR>";
					output += "<TABLE border='1'><TR><TH>Log</TH><TH>Formatted</TH><TH>Raw</TH></TR>";
					String[] logs = h.log.getLogs();
					for (int x=0; x < logs.length; x++)
					{
						output += "<TR><TD>"+logs[x]+"</TD><TD align='center'><A HREF='/log?logfile="+logs[x]+"'>View</A></TD><TD align='center'><A HREF='/rawlog?logfile="+logs[x]+"'>View</A></TD></TR>";
					}
					output += "</TABLE>";
				}
				else if (url.equals("/crossdomain.xml"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/xml\n";
					output += "Server: Test Server\n\n";
					//out.println();
					output += "<?xml version=\"1.0\"?>\n";
					output += "<!-- http://www.foo.com/crossdomain.xml -->\n";
					output += "<cross-domain-policy>\n";
					output += "    <allow-access-from domain=\"*\"/>\n";
					output += "</cross-domain-policy>\n";
				}
				else if (url.length() > 6 && url.substring(0, 7).equals("/toggle"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/html\n";
					output += "Server: Test Server\n\n";
					//out.println();
					output += "<html><head><title>"+h.label+": Check Toggle</title></head><body>\n";
					output += "<STYLE>BODY {font-family:verdana;font-size:12px}</STYLE>\n";
					int checkid = Integer.valueOf(variables.get("check").toString());
					
					
					if (h.monitorList.get(checkid).getThisThread().isAlive())
					{
						h.monitorList.get(checkid).disablemonitor();
						output += "Check has been disabled...\n";
						h.log.logit("Check -"+h.monitorList.get(checkid).getStatus().label+"- has been disabled by "+session.getRemoteAddress(),"disable",h.monitorList.get(checkid).getStatus());
						
					}
					else
					{
						//if (h.monitorList.get(checkid).getThisThread()
						h.monitorList.get(checkid).regenerate();
						output += "Check has been enabled & started...\n";
						h.log.logit("Check -"+h.monitorList.get(checkid).getStatus().label+"- has been enabled by "+session.getRemoteAddress(),"enable-"+h.monitorList.get(checkid).getStatus().successrate+"-"+h.monitorList.get(checkid).getStatus().retryrate,h.monitorList.get(checkid).getStatus());
						
					}
					output += "<BR><BR><A HREF='/status'>Back to Status</A>";
					output += "</body></html>";
				}
				else if (url.equals("/status"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/html\n";
					output += "Server: Test Server\n\n";
					//out.println();

					output += "<html><head><title>"+h.label+": Path Status</title></head><body>\n";
					output += "<STYLE>BODY {font-family:verdana;font-size:12px} TH {font-family:verdana;font-size:12px} TD {font-family:verdana;font-size:12px}</STYLE>\n";
					output += "<H2>Node Status: "+h.label+"</H2>\n";
					
					output += "<A HREF='/'>Home</A> - <A HREF='/listlogs'>View Logs</A> - <A HREF='/xmlstatus'>View as XML</A> - <A HREF='/csvstatus'>View as CSV</A> - <A HREF='/xmlstatus2'>XCelcius-Friendly Status (XML)</A> - <A HREF='/reload'>Reload Spec File (/reload) - Use only if necessary</A><BR><BR>\n";
					output += "<TABLE border='1'><TR><TH>Status</TH><TH>Label</TH><TH>Type</TH><TH>Host</TH><TH>Check Rate</TH><TH>Retry Rate</TH><TH>Last Update</TH><TH>Last Failure</TH><TH>Toggle</TH></TR>\n";
					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{
							MonitorStatus s = h.monitorList.get(i).getStatus();
							output += "<TR>\n";
							if (!s.enabled)
							{
								output += "<TD bgcolor='gray' align='center'>DISABLED</TD>\n";
							}
							else if (s.pass)
							{
								output += "<TD bgcolor='green' align='center'>PASS</TD>\n";
							}
							else
							{
								output += "<TD bgcolor='red' align='center'>FAILURE</TD>\n";
							}
							output += "<TD>"+s.label+"</TD>\n";
							output += "<TD>"+s.checktype+"</TD>\n";
							output += "<TD>"+s.host+"</TD>\n";
							output += "<TD align='center'>"+s.successrate+"</TD>\n";
							output += "<TD align='center'>"+s.retryrate+"</TD>\n";
							output += "<TD>"+s.lastupdate+"</TD>\n";
							output += "<TD>"+s.lastfailure+"</TD>\n";
							if (!s.enabled)
							{
								output += "<TD><A HREF='/toggle?check="+i+"'>Enable</A></TD>\n";	
							}
							else
							{
								output += "<TD><A HREF='/toggle?check="+i+"'>Disable</A></TD>\n";
							}
							
							output += "</TR>\n";
						}
					}
					output += "</TABLE><BR><I>Monitoring Started/Reloaded on: "+h.startstamp+"</I>\n";
					output += "</body></html>\n";
				}
				else if (url.equals("/csvstatus"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/plain\n";
					output += "Server: Test Server\n\n";
					//out.println();

					output += "Status,Label,Type,Host,Check Rate,Retry Rate,Last Update,Last Failure\n";
					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{
							MonitorStatus s = h.monitorList.get(i).getStatus();
							if (!s.enabled)
							{
								output += "DISABLED,";
							}
							else if (s.pass)
							{
								output += "PASS,";
							}
							else
							{
								output += "FAILURE,";
							}
							output += s.label+",";
							output += s.checktype+",";
							output += s.host+",";
							output += s.successrate+",";
							output += s.retryrate+",";
							output += s.lastupdate+",";
							output += s.lastfailure+"\n";
						}
					}
				}
				else if (url.equals("/xmlstatus"))
				{
					//h.log.logit("Start XML", "test", null);
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/xml\n";
					output += "Server: Test Server\n\n";
					//out.println();

					output += "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
					output += "<summary label=\""+h.label+"\" nodeid=\""+h.nodeid+"\" started=\""+h.startstamp+"\">\n";
					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{
							output += "<check>\n";
							output += "<node>"+h.label+"</node>\n";
							MonitorStatus s = h.monitorList.get(i).getStatus();
							if (!s.enabled)
							{
								output += "<status>DISABLED</status>\n";
							}
							else if (s.pass)
							{
								output += "<status>PASS</status>\n";
							}
							else
							{
								output += "<status>FAILURE</status>\n";
							}
							output += "<label>"+s.label+"</label>\n";
							output += "<checktype>"+s.checktype+"</checktype>\n";
							output += "<host>"+s.host+"</host>\n";
							output += "<successrate>"+s.successrate+"</successrate>\n";
							output += "<retryrate>"+s.retryrate+"</retryrate>\n";
							output += "<lastupdate>"+s.lastupdate+"</lastupdate>\n";
							output += "<lastfailure>"+s.lastfailure+"</lastfailure>\n";
							output += "</check>\n";
						}
					}
					output += "</summary>\n";
					//h.log.logit("End XML", "test", null);
				}
				else if (url.equals("/xmlstatus2"))
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/xml\n";
					output += "Server: Test Server\n\n";
					//out.println();

					int errorcount = 0;
					int totalcount = 0;

					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{	MonitorStatus s = h.monitorList.get(i).getStatus();
							if (!s.pass && s.enabled)
							{
								errorcount++;
							}
							totalcount++;
						}
					}					
					
					output += "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
					output += "<data>\n";
					output += "<variable name=\"checkcount\"><row><column>"+totalcount+"</column></row></variable>\n";
					output += "<variable name=\"startstamp\"><row><column>"+h.startstamp+"</column></row></variable>\n";
					output += "<variable name=\"errorcount\"><row><column>"+errorcount+"</column></row></variable>\n";
					output += "<variable name=\"nodelabel\"><row><column>"+h.label+"</column></row></variable>\n";
					output += "<variable name=\"checkdata\">\n";
					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{	MonitorStatus s = h.monitorList.get(i).getStatus();
							output += "<row>";
							if (!s.enabled)
							{
								output += "<column>DISABLED</column>\n";
							}
							else if (s.pass)
							{
								output += "<column>PASS</column>\n";
							}
							else
							{
								output += "<column>FAILURE</column>\n";
							}							
							output += "<column>"+s.pass+"</column>\n";
							output += "<column>"+s.label+"</column>\n";
							output += "<column>"+s.host+"</column>\n";
							output += "<column>"+s.lastupdate+"</column>\n";
							output += "<column>"+s.lastfailure+"</column>\n";
							output += "</row>\n";
						}
					}	
					output += "</variable>\n";
					output += "</data>\n";
				}				
				else if (url.equals("/reload"))
				{
					out2.println("HTTP/1.0 200 OK");
					out2.println("Content-Type: text/html");
					out2.println("Server: Test Server");
					out2.println();
					out2.println("<html><head><title>"+h.label+": Reloading</title></head><body>");
					out2.println("Reloading Configuration File...<BR><BR>");
					out2.println("<A HREF='/status'>Visit Status Page</A></body></html>");
					out2.flush();
					
					out2.close();
					
					// Sending out a warning to heartbeat clients that there is going to be a reset
					for (int i=0;i < h.sessions.size();i++)
					{
						try
						{
							h.sessions.get(i).write("goingtoreload");
						}
						catch (Exception e)
						{
						}
					}
					
					System.out.println("Initiating reload...");
					h.log.mailerset = false;
					h.log.l1mailerset = false;
					h.log.l2mailerset = false;

					for (int i=0;i < h.monitorList.size();i++)
					{
						if (h.monitorList.get(i) != null)
						{
							h.monitorList.get(i).stopmonitor();
						}
					}
					
					h.heartbeatAcceptor.unbind();
					h.heartbeatAcceptor.dispose();

					h.log.logit("System reloading spec file. Initiated by "+session.getRemoteAddress(),"reload",null);
					h.log.close();
					
					h.log.pinger.stopPing();

					h.statusAcceptor.unbind();
					h.statusAcceptor.dispose();
					Runtime.getRuntime().gc();
				}
				else
				{
					output += "HTTP/1.0 200 OK\n";
					output += "Content-Type: text/html\n";
					output += "Server: Test Server\n\n";
					//out.println();
					output += "<STYLE>BODY {font-family:verdana;font-size:12px}</STYLE>\n";
					output += "<html><head><title>"+h.label+": Node Monitor</title></head><body>\n";
					output += "<H3>Available URL Commands http://host/command</H3><UL>\n";
					output += "<LI><A HREF='/status'>Human Readable Status (/status)</A><BR>\n";
					output += "<LI><A HREF='/csvstatus'>CSV Status (/csvstatus)</A><BR>\n";
					output += "<LI><A HREF='/xmlstatus'>XML Status (/xmlstatus)</A><BR>\n";
					output += "<LI><A HREF='/xmlstatus2'>XCelcius-Friendly XML Status (/xmlstatus2)</A><BR>\n";
					output += "<LI><A HREF='/listlogs'>View Logs (/listlogs)</A><BR>\n";
					//output += "<LI><A HREF='/log'>HTML Formatted Log (/log)</A><BR>\n";
					//output += "<LI><A HREF='/rawlog'>Raw Log (/rawlog)</A><BR>\n";
					output += "<LI><A HREF='/reload'>Reload Spec File (/reload) - Use only if necessary</A><BR>\n";
					output += "</UL></body></html>\n";						
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				//h.log.logit("Exception 1", "test", null);
			}
			finally
			{
				//h.log.logit("Output: "+output, "test", null);
				try
				{
					//h.log.logit("Appending...", "test", null);
					out.append(output);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				try
				{
					//h.log.logit("Flushing...", "test", null);
					//out.flush();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				
				try
				{
					//h.log.logit("Closing...", "test", null);
					out.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				//h.log.logit("Step 0.1 - "+out.checkError(), "test", null);
				
				//h.log.logit("Step 1", "test", null);
				//out.flush();
				//h.log.logit("Step 2", "test", null);
				//out.close();
				//h.log.logit("Step 3", "test", null);
				try
				{
					in.close();
					//h.log.logit("Step 4", "test", null);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					//h.log.logit("Exception 2", "test", null);
				}
			}
			//h.log.logit("Run End", "test", null);
		}
	}
}
