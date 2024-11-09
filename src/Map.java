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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class Map extends JLayeredPane
{
    private ImageIcon thePhone = new ImageIcon("images/phone.png"); 
    private float theLongitude1, theLatitude1,  
        theLongitude2, theLatitude2, 
        thePixelX1, thePixelY1,
        thePixelX2, thePixelY2;
    private AbstractMap theUsers = new HashMap();
    private int theNextColor; 
    private Main itsTracker;

    public Map(Properties aConfig, Main aTracker)
    {
        itsTracker = aTracker;
        JComponent map = new JLabel(new ImageIcon(aConfig.getProperty("map.image")));
        JComponent areas = new Areas();
        add(areas, JLayeredPane.DEFAULT_LAYER);
        add(map, JLayeredPane.DEFAULT_LAYER);

        setPreferredSize(map.getPreferredSize());
        map.setSize(map.getPreferredSize());
        areas.setSize(map.getPreferredSize());

        theLatitude1 = Float.parseFloat(aConfig.getProperty("map.latitude1"));
        theLongitude1 = Float.parseFloat(aConfig.getProperty("map.longitude1"));
        theLatitude2 = Float.parseFloat(aConfig.getProperty("map.latitude2"));
        theLongitude2 = Float.parseFloat(aConfig.getProperty("map.longitude2"));
        thePixelX1 = 0;
        thePixelY1 = 0;
        thePixelX2 = map.getPreferredSize().width;
        thePixelY2 = map.getPreferredSize().height;
    }

    synchronized public void addLabel(String aLabel)
    {
        JComponent user = (JComponent) theUsers.get(aLabel);
        if (user == null)
        {
            user = new User(thePhone);
            user.setToolTipText(aLabel);
            theUsers.put(aLabel, user);
            user.setVisible(false);
            add(user, JLayeredPane.MODAL_LAYER);
            user.setSize(user.getPreferredSize());
        }
    }

    synchronized public void moveLabel(String aLabel, float latitude, float longitude)
    {
        JComponent user = (JComponent) theUsers.get(aLabel);
		
        if (user != null)
        {
            Point p = toPixels(latitude, longitude);
            p.x -= user.getSize().width / 2;
            p.y -= user.getSize().height / 2;
            user.setLocation(p.x, p.y);
            user.setVisible(true);
            calcDistances();
            repaint(100);
        }
    }

    private synchronized void calcDistances()
    {
        for (Iterator i = theUsers.keySet().iterator(); i.hasNext();)
        {
            String label1 = (String) i.next();
            User c1 = (User) theUsers.get(label1);
            Rectangle r1 = c1.getBounds();
            Point p1 = new Point(r1.x + r1.width / 2,
                r1.y + r1.height / 2);
            for (Iterator j = theUsers.keySet().iterator(); j.hasNext();)
            {
                String label2 = (String) j.next();
                if (label1.equals(label2))
                {
                    continue;
                }
                User c2 = (User) theUsers.get(label2);
                Rectangle r2 = c2.getBounds();
                Point p2 = new Point(r2.x + r2.width / 2,
                    r2.y + r2.height / 2);
                int sqrDist = sqr(p1.x - p2.x) + sqr(p1.y - p2.y);
                boolean near = sqrDist < 4 * 40 * 40;
                setNeighbors(label1, label2, near);
            }
        }
    }

    private void setNeighbors(String l1, String l2, boolean near)
    {
        User u1 = (User) theUsers.get(l1);
        User u2 = (User) theUsers.get(l2);
        if (near)
        {
            if (u1.addNeighbor(u2))
            {
                System.out.println(l1 + " is near to " + l2);
                itsTracker.notifyNewNeighbors(l1, l2);
            }
            if (u2.addNeighbor(u1))
            {
                itsTracker.notifyNewNeighbors(l2, l1);
            }
        }
        else
        {
            u1.removeNeighbor(u2);
            u2.removeNeighbor(u1);
        }
    }

    private int sqr(int x)
    {
        return x * x;
    }

    synchronized public void removeLabel(String aLabel)
    {
        JComponent user = (JComponent) theUsers.remove(aLabel);
        if (user != null)
        {
            remove(user);
            repaint(100);
        }
    }

    public Point toPixels(float latitude, float longitude)
    {
        Point p = new Point();
        p.x = (int) ((latitude - theLatitude1)
                * (thePixelX2 - thePixelX1)
                / (theLatitude2 - theLatitude1)
            + thePixelX1);
        p.y = (int) ((longitude - theLongitude1)
                * (thePixelY2 - thePixelY1)
                / (theLongitude2 - theLongitude1)
            + thePixelY1);
        return p;
    } class Areas extends JPanel
    {
        public Areas()
        {
            setOpaque(false); 
        }

        public void paint(Graphics g)
        {
            super.paint(g);
            int R = 40;
            int D = 2 * R;
            g.setColor(new Color(160, 160, 160, 128));
            for (Iterator i = theUsers.values().iterator(); i.hasNext();)
            {
                JComponent label = (JComponent) i.next();
                Rectangle bounds = label.getBounds();
                int x = bounds.x + bounds.width / 2;
                int y = bounds.y + bounds.height / 2;
                g.fillOval(x - R, y - R, D, D);
            }
        }
    } class User extends JLabel
    {
        Set theNeighbors = new HashSet();

        public User(ImageIcon i)
        {
            super(i);
        }

        public boolean addNeighbor(User u)	
        {
            return theNeighbors.add(u);
        }

        public void removeNeighbor(User u)	
        {
            theNeighbors.remove(u);
        }
    }
}
