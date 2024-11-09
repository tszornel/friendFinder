/*
 * **************************************************************************
 * *                                                                        *
 * * Ericsson hereby grants to the user a royalty-free, irrevocable,        *
 * * worldwide, nonexclusive, paid-up license to copy, display, perform,    *
 * * prepare and have prepared derivative works based upon the source code  *
 * * in this sample application, and distribute the sample source code and  *
 * * derivative works thereof and to grant others the foregoing rights.     *
 * *                                                                        *
 * * ERICSSON DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,        *
 * * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.       *
 * * IN NO EVENT SHALL ERICSSON BE LIABLE FOR ANY SPECIAL, INDIRECT OR      *
 * * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS    *
 * * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE  *
 * * OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE *
 * * OR PERFORMANCE OF THIS SOFTWARE.                                       *
 * *                                                                        *
 * **************************************************************************
 */

import com.ericsson.hosasdk.utility.framework.FWproxy;
import java.util.*;
import java.io.*;
import com.ericsson.hosasdk.api.*;
import com.ericsson.hosasdk.api.mm.*;
import com.ericsson.hosasdk.api.mm.us.*; 
import com.ericsson.hosasdk.api.mm.ul.*; 
import com.ericsson.hosasdk.utility.sync.Synchronizer;
import com.ericsson.hosasdk.utility.log.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Main
{
    private FWproxy itsFramework;
    private LocationTracker itsLocationTracker;
    private Messenger itsMessenger;
    private Map itsMap;
    private Object LOCK = new Object();
    private boolean itsTerminated;

    public static void main(String[] args)
        throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream("config/config.ini"));
        Main t = new Main(props);
        t.addUser("1");
        t.addUser("2");
    }

    public Main(Properties aConfig)
    {
        itsMap = new Map(aConfig, this);
        final JFrame gui = new JFrame();
        gui.setTitle("Friend Finder");
        gui.getContentPane().setLayout(new BorderLayout());
        gui.getContentPane().add(new JLabel("This application tracks the location of phones '1' and '2'."),
            BorderLayout.SOUTH);
        gui.getContentPane().add(itsMap, BorderLayout.CENTER);
        gui.pack();
        gui.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                try
                {
                    dispose();
                }
                finally
                {
			  System.exit(0);
                }
            }
        });
        gui.setVisible(true);
        SimpleTracer.SINGLETON.PRINT_STACKTRACES = false;
        HOSAMonitor.addListener(SimpleTracer.SINGLETON);

        itsFramework = new FWproxy(aConfig);

        itsLocationTracker = new LocationTracker(this, itsFramework);
        itsMessenger = new Messenger(this, itsFramework);
    }

    private void dispose()
    {
        itsTerminated = true;
        try
        {
            Thread.sleep(500);
        }
        catch (Exception e)
        {}
        if (itsLocationTracker != null) 
        {
            synchronized (LOCK)
            {
                itsLocationTracker.dispose();
                itsLocationTracker = null;
            }
        }
        if (itsMessenger != null) 
        {
            itsMessenger.dispose();
            itsMessenger = null;
        }

        if (itsFramework != null)
        {
            itsFramework.endAccess(null);
            itsFramework.dispose();
            itsFramework = null;
        }
    }

    public void addUser(final String aUser)
    {
        itsMap.addLabel(aUser);
        new Thread()
        {
            public void run()
            {
                try
                {
                    while (true)
                    {
                        synchronized (LOCK)
                        {
                            if (itsTerminated)
                            {
                                break;
                            }
                            itsLocationTracker.getLocation(aUser);
                        }
                        Thread.sleep(5000);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void notifyNewNeighbors(String u1, String u2)
    {
        itsMessenger.sendSMS(u1, u2, u1 + " is nearby.");
    }

    public void notifyLocation(String aUser, float latitude, float longitude)
    {
        // System.out.println("User '" + aUser + "' moved to " + latitude + "' " + longitude + "\"");
        itsMap.moveLabel(aUser, latitude, longitude);
    }
}
