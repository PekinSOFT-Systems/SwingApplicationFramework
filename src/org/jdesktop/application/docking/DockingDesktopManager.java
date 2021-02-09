/*
 * Copyright (C) 2021 PekinSOFT Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * *****************************************************************************
 * Project    :   Northwind
 * Class      :   DockingDesktopManager.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 9:52:14 AM
 * Modified   :   Jan 4, 2021
 *  
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 4, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application.docking;

import org.jdesktop.application.docking.DockConstraints.DockLocation;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.EAST;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.NORTH;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.SOUTH;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.WEST;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.LayoutManager2;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JInternalFrame.JDesktopIcon;
import javax.swing.JLayeredPane;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class DockingDesktopManager implements DesktopManager {

	public void openFrame(JInternalFrame frame) {
    	if (frame.getDesktopIcon().getParent() != null) {
    		Container parent = frame.getDesktopIcon().getParent();
    		if (parent != null) {
    			parent.add(frame);
    		}
    		removeIcon(frame);
    	}
    }

    public void closeFrame(JInternalFrame frame) {
    	Component child = getChild(frame);
		JDesktopPane parent = (JDesktopPane)child.getParent();
		if (parent == null) {
			return;
		}
    	selectNextFrame(frame);
        if (parent != null) {
            parent.remove(child);
        }
        removeIcon(frame);
        if (parent.getAllFrames().length == 0) {
        	parent.requestFocus();
        }
    }
    
    private void selectNextFrame(JInternalFrame frame) {
    	if (frame.isSelected()) {
        	JInternalFrame nextFrame = getNextFrame(frame);
        	setSelected(nextFrame);
        }
    }
        
    protected void setSelected(JInternalFrame frame) {
    	try {
			frame.setSelected(true);
		} catch (PropertyVetoException ignore) {
		}
		frame.restoreSubcomponentFocus();
    }
    
    private JInternalFrame getNextFrame(JInternalFrame frame) {
    	JDesktopPane desktop = frame.getDesktopPane();
    	JInternalFrame[] frames = desktop.getAllFrames();
    	boolean found = false;
    	for (JInternalFrame f: frames) {
    		if (found) {
    			return f;
    		}
    		if (frame == f) {
    			found = true;
    		}
    	}
    	return frames.length == 0? null: frames[0];
    }
    
    private void removeIcon(JInternalFrame frame) {
    	JDesktopIcon icon = frame.getDesktopIcon();
    	Container parent = icon.getParent();
    	if (parent != null) {
    		parent.remove(icon);
    	}
    }

    public void maximizeFrame(JInternalFrame frame) {
        if (frame.isIcon()) {
            try {
                frame.setIcon(false);
            } catch (PropertyVetoException ignore) {
            }
        } else {
        	Component child = getChild(frame);
    		Container parent = child.getParent();
    		DockLayout layout = (DockLayout)parent.getLayout();
    		layout.maximizeLayoutComponent(child);
    		((JLayeredPane)parent).moveToFront(child);
        }
        setSelected(frame);
    }

    public void minimizeFrame(JInternalFrame frame) {
        if (frame.isIcon()) {
            iconifyFrame(frame);
            return;
        }
    	Component child = getChild(frame);
		Container parent = child.getParent();
		DockLayout layout = (DockLayout)parent.getLayout();
		layout.minimizeLayoutComponent(child);
    }

    public void iconifyFrame(JInternalFrame frame) {
        JDesktopIcon icon = frame.getDesktopIcon();
        JDesktopPane desktop = frame.getDesktopPane();
        DockLayout layout = (DockLayout)desktop.getLayout();
        if (frame.isMaximum()) {
        	layout.minimizeLayoutComponent(frame);
        }
        if (frame.isSelected()) {
        	selectNextFrame(frame);
        }
        frame.setVisible(false);
        desktop.add(icon);
    }

    public void deiconifyFrame(JInternalFrame frame) {
        JDesktopIcon icon = frame.getDesktopIcon();
        Container parent = icon.getParent();
        if (parent != null) {
        	parent.remove(icon);
        	for (Component component: parent.getComponents()) {
        		if (component instanceof JInternalFrame && ((JInternalFrame)component).isMaximum()) {
        			try {
						((JInternalFrame)component).setMaximum(false);
						frame.setMaximum(true);
					} catch (PropertyVetoException e) {
						//ignore
					}
        		}
        	}
        }
        frame.setVisible(true);
        if (frame.isMaximum()) {
        	DockLayout layout = (DockLayout)frame.getParent().getLayout();
        	layout.maximizeLayoutComponent(frame);
        }
        setSelected(frame);
    }

    public void activateFrame(JInternalFrame frame) {
    	Component child = getChild(frame);
    	JDesktopPane parent = (JDesktopPane)child.getParent();
    	JInternalFrame currentlyActiveFrame = parent == null? null: parent.getSelectedFrame();
    	if (parent == null) {
    		parent = (JDesktopPane)getChild(frame.getDesktopIcon()).getParent();
    		if (parent == null) {
    			return;
    		}
    	}
    	if (currentlyActiveFrame == null) {
    		if (parent != null) {
    			parent.setSelectedFrame(frame);
    		}
    	} else if (currentlyActiveFrame != frame) {  
    		if (currentlyActiveFrame.isSelected()) { 
    			try {
    				currentlyActiveFrame.setSelected(false);
    			} catch(PropertyVetoException ignore) {
    			}
    		}
    		if (parent != null) {
    			parent.setSelectedFrame(frame);
    		}
    	}
    	frame.moveToFront();
    }
    
    public void deactivateFrame(JInternalFrame frame) {
    	JDesktopPane desktop = frame.getDesktopPane();
    	JInternalFrame currentlyActiveFrame = desktop == null? null: desktop.getSelectedFrame();
    	if (currentlyActiveFrame == frame) {
    		desktop.setSelectedFrame(null);
    	}
    }

    protected transient Rectangle dragBounds;
    
    public void beginDraggingFrame(JComponent frame) {
    	dragBounds = frame.getBounds();
    	drawBounds(frame);
    }

    public void dragFrame(JComponent frame, int newX, int newY) {
    	//We don't rely on the specified coordinates,
    	//because some UIs ensure these coordinates to be within the dragged frame
    	Point point = getMouseLocation(getDesktopPane(frame));
    	drawBounds(frame);
    	Component component = getDesktopPane(frame).getComponentAt(point.x, point.y);
    	if (component == frame) {
    		frame.getBounds(dragBounds);
    		dragBounds.width = 0;
    		dragBounds.height = 0;
    	} else if (component != null) {
    		calculateDragBounds(component, point.x, point.y);
    	}
    	drawBounds(frame);
    }
    
    protected void calculateDragBounds(Component dragDestination, int x, int y) {
    	DockLocation location = getDockLocation(dragDestination, x, y);
    	dragDestination.getBounds(dragBounds);
    	switch (location) {
    	case SOUTH:
    		dragBounds.y += dragBounds.height / 2;
    	case NORTH:
    		dragBounds.height /= 2;
    		break;
    	case EAST:
    		dragBounds.x += dragBounds.width / 2;
    	case WEST:
    		dragBounds.width /= 2;
    		break;
    	}    	
    }

    public void endDraggingFrame(JComponent frame) {
    	drawBounds(frame);
    	moveFrame(frame, dragBounds.x + dragBounds.width / 2, dragBounds.y + dragBounds.height / 2);
    }
    
    public void moveFrame(JComponent frame, int newX, int newY) {
		JDesktopPane desktop = getDesktopPane(frame);
		LayoutManager2 layoutManager = (LayoutManager2)desktop.getLayout();
		Component componentToDock = desktop.getComponentAt(newX, newY);
		if (componentToDock != null && componentToDock != frame) {
			layoutManager.addLayoutComponent(frame, new DockConstraints(componentToDock, getDockLocation(componentToDock, newX, newY)));
			layoutManager.layoutContainer(desktop);
		}
		if (frame instanceof JInternalFrame) {
			setSelected((JInternalFrame)frame);
		}
    }

    public void beginResizingFrame(JComponent frame, int direction) {
    	dragBounds = frame.getBounds();
    	drawBounds(frame);
    }

    public void resizeFrame(JComponent frame, int newX, int newY, int newWidth, int newHeight) {
    	drawBounds(frame);
    	dragBounds.setBounds(newX, newY, newWidth, newHeight);
    	drawBounds(frame);
    }

    public void endResizingFrame(JComponent frame) {
    	drawBounds(frame);
    	setBoundsForFrame(frame, dragBounds.x, dragBounds.y, dragBounds.width, dragBounds.height);
    }

    protected void drawBounds(JComponent frame) {
    	JDesktopPane desktop = getDesktopPane(frame);
    	Graphics graphics = desktop.getGraphics();
    	graphics.setXORMode(Color.WHITE);
    	if (dragBounds.width == 0 || dragBounds.height == 0) {
    		graphics.drawLine(dragBounds.x, dragBounds.y, dragBounds.x + dragBounds.width, dragBounds.y + dragBounds.height);
    	} else {
    		graphics.drawRect(dragBounds.x, dragBounds.y, dragBounds.width, dragBounds.height);
    	}
    }

    public JDesktopPane getDesktopPane(JComponent frame) {
    	Component parent = frame.getParent();
    	while (parent != null && !(parent instanceof JDesktopPane)) {
    		parent = parent.getParent();
    	}
    	return (JDesktopPane)parent;
    }

    public void setBoundsForFrame(JComponent frame, int newX, int newY, int newWidth, int newHeight) {
    	if (newWidth == frame.getWidth() && newHeight == frame.getHeight()) {
    		moveFrame(frame, newX + newWidth / 2, newY + newHeight / 2);
    	} else {
    		JDesktopPane parent = getDesktopPane(frame);
    		DockLayout layout = (DockLayout)parent.getLayout();
    		if (newX == 0 && newY == 0 && newWidth == parent.getWidth() && newHeight == parent.getHeight()) {
    			layout.maximizeLayoutComponent(frame);
    		} else {
    			layout.minimizeLayoutComponent(frame);
    			layout.resizeLayoutComponent(frame, newX, newY, newWidth, newHeight);
    		}
    		frame.validate();
    	}
    }
    
    private Point getMouseLocation(Component component) {
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		Point componentLocation = component.getLocationOnScreen();
		mouseLocation.translate(-componentLocation.x, -componentLocation.y);
		return mouseLocation;
    }
    
    private DockLocation getDockLocation(Component component, int x, int y) {
    	if (!component.getBounds().contains(x, y)) {
    		throw new IllegalArgumentException("location must be within component");
    	}
    	float dx = (float)(x - component.getX()) / component.getWidth();
    	float dy = (float)(y - component.getY()) / component.getHeight();
    	if (Math.abs(dx - 0.5) > Math.abs(dy - 0.5)) {
    		if (dx < 0.5) {
    			return DockLocation.WEST;
    		} else {
    			return DockLocation.EAST;
    		}
    	} else {
    		if (dy < 0.5) {
    			return DockLocation.NORTH;
    		} else {
    			return DockLocation.SOUTH;
    		}    		
    	}
    }
    
    /**
     * Returns the direct child of a <tt>JDesktopPane</tt> that is
     * the parent of the specified component.
     * @param component
     * @return the specified component or one of its parents that is a direct child of a JDesktopPane.
     */
    protected Component getChild(Component component) {
    	Component child = component;
		Container parent = component.getParent();
		while (parent != null && !(parent instanceof JDesktopPane)) {
			child = parent;
			parent = child.getParent();
		}
		return child;
    }
}
