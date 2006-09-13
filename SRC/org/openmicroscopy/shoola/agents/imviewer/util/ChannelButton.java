/*
 * org.openmicroscopy.shoola.agents.imviewer.util.ChannelButton
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.imviewer.util;


//Java imports
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;


//Third-party libraries

//Application-internal dependencies

/** 
 * Customized button used to select the rendered channel.
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">a.falconi@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $ $Date: $)
 * </small>
 * @since OME2.2
 */
public class ChannelButton
    extends JButton
    implements PropertyChangeListener
{
    
    /** Bound property indicating to bring up the info dialog. */
    public static final String  INFO_PROPERTY = "info";
    
    /** 
     * Bound property name indicating that the channel is or is not selected. 
     */
    public static final String  CHANNEL_SELECTED_PROPERTY = "channelSelected";
    
    /** 
     * Bound property name indicating that the channel is mapped to a new color. 
     */
    public static final String  CHANNEL_COLOR_PROPERTY = "channelColor";
    
    /** The border set when the channel is selected. */
    public static final Border  SELECTED_BORDER = 
                    BorderFactory.createBevelBorder(BevelBorder.RAISED);
    
    /** The border set when the channel is not selected. */
    public static final Border  DESELECTED_BORDER = 
        BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    
    /** The OME index of the channel. */
    private final int index;
    
    /** Fires an event to select the channel. */
    private final void setChannelSelected()
    {
        Boolean value = null;
        if (isSelected()) value = Boolean.FALSE;
        else value = Boolean.TRUE;
        HashMap map = new HashMap(1);
        map.put(new Integer(index), value);
        firePropertyChange(CHANNEL_SELECTED_PROPERTY, null, map);
    }
    
    /**
     * Fires property depending on the keys pressed.
     * If Ctrl+ left click, we bring the color picker.
     * If Ctrl+right click, we bring the info button.
     * otherwise the select the channel.
     * 
     * @param e         The mouse event to handle.
     * @param released  Pass <code>true</code> if the method is invoked when 
     *                  the mouse is released, pass <code>false</code> if the 
     *                  mouse is pressed.
     */
    private void onClick(MouseEvent e, boolean released)
    {
        //Ctrl+ left click to bring color picker
        //Ctrl+right click to bring up info
        if (e.isControlDown()) {
            if (e.isPopupTrigger()) { //info
                firePropertyChange(INFO_PROPERTY, null, new Integer(index));
            } else { //color picker
                
            }
        } else {
            if (!released) setChannelSelected();
        }
    }
    
    /**
     * Creates a new instance.
     * 
     * @param text      The text of the button. The text should correspond to
     *                  the emission wavelength.
     * @param color     The background color of the button. Corresponds to the
     *                  color associated to the channel.
     * @param index     The channel index.
     * @param selected  Passed <code>true</code> to select the channel (i.e.
     *                  the channel is rendered), <code>false</code> otherwise
     *                  (i.e. the channel is not rendered.)
     */
    public ChannelButton(String text, Color color, int index, 
                        boolean selected)
    {
        super(text);
        if (color == null) 
            throw new IllegalArgumentException("No color.");
        this.index = index;
        setEnabled(false);
        setBackground(color);
        setUI(new ChannelButtonUI());
        setRolloverEnabled(false);
        setSelected(selected);
        addMouseListener(new MouseAdapter() {
            //need the 2  b/c we use the method e.isPopupTrigger() 
            public void mousePressed(MouseEvent e) { onClick(e, false); }
            
            public void mouseReleased(MouseEvent e) { onClick(e, true); }
        });
    }
    
    /**
     * Creates a deselected button. 
     * 
     * @param text      The text of the button. The text should correspond to
     *                  the emission wavelength.
     * @param color     The background color of the button. Corresponds to the
     *                  color associated to the channel.
     * @param index     The channel index.
     */
    public ChannelButton(String text, Color color, int index)
    {
        this(text, color, index, false);
    }
    
    /**
     * Overriden to set the border depending on the selection state.
     * @see AbstractButton#setSelected(boolean)
     */
    public void setSelected(boolean b)
    {
        if (b) setBorder(DESELECTED_BORDER);
        else setBorder(SELECTED_BORDER); 
        super.setSelected(b);
    }
    
    /**
     * Returns the index of the channel.
     * 
     * @return See above.
     */
    public int getChannelIndex() { return index; }
    
    /**
     * Nested class used to set the color of the button when a mouse pressed
     * event occured.
     * @see BasicButtonUI#paintButtonPressed(Graphics, AbstractButton)
     */
    private class ChannelButtonUI
        extends BasicButtonUI
    {
        protected void paintButtonPressed(Graphics g, AbstractButton b) {
            b.setBackground(b.getBackground());
        }
    }

    /**
     * Reacts to color selection in the ColorChooser
     * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent pce)
    {
        //Will listen to a property fired by ColorChooser
        Map map = (Map) pce.getNewValue();
        Object o = map.get(new Integer(index));
        if (o != null) {
            setBackground((Color) o);
            firePropertyChange(CHANNEL_COLOR_PROPERTY, null, map);
        }
    }

}
