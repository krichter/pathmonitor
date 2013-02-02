
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import java.util.Vector;

public class HeartBeatHandler extends IoHandlerAdapter
{
	private Vector<IoSession> sessions;
	
	public HeartBeatHandler(Vector<IoSession> setSessions)
	{
		sessions = setSessions;
	}

	public void sessionOpened(IoSession session)
	{
		sessions.add(session);
	}
	
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		//cause.printStackTrace();
		sessions.remove(session);
		session.close();
	}
	
	public void sessionClosed(IoSession session)
	{
		sessions.remove(session);
	}
	
	public void messageReceived(IoSession session, Object message)
	{
		//System.out.println("Mirror-thread Message Received: "+message.toString());
        String str = message.toString();
        StringBuffer buf = new StringBuffer(str.length());
        for (int i = str.length() - 1; i >= 0; i--) {
            buf.append(str.charAt(i));
        }
        //System.out.println(str);

        // and write it back.
        session.write(buf.toString());
	}
}
