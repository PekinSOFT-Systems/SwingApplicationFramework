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
 * Class      :   TabbingDesktopManager.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 10:09:56 AM
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

package com.pekinsoft.desktop.docking;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JInternalFrame.JDesktopIcon;
import javax.swing.JLayeredPane;
import javax.swing.JTabbedPane;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class TabbingDesktopManager extends DockingDesktopManager {

	public static final String TABBED_PANE_PROPERTY = TabbingDesktopManager.class.getName() + ".internalFrame.tabbedPane";
	public static final String INTERNAL_FRAME_PROPERTY = TabbingDesktopManager.class.getName() + ".tabbedPane.internalFrame";

    public void closeFrame(JInternalFrame frame) {
    	Component child = getChild(frame);
    	if (child instanceof DockingTabbedPane) {
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
    		int tabIndex = tabbedPane.getTabIndex(frame);
    		tabbedPane.removeTabAt(tabIndex);
    		handleRemoveLastTab(tabbedPane);
    	} else if (frame.getClientProperty(TABBED_PANE_PROPERTY) != null) {
    		JTabbedPane tabbedPane = (JTabbedPane)frame.getClientProperty(TABBED_PANE_PROPERTY);
    		JDesktopPane desktop = frame.getDesktopPane();
    		DockLayout dockLayout = (DockLayout)desktop.getLayout();
    		dockLayout.replaceLayoutComponent(frame, tabbedPane);
    		super.closeFrame(frame);
    		desktop.add(tabbedPane);
    		tabbedPane.setVisible(false);
    		tabbedPane.putClientProperty(INTERNAL_FRAME_PROPERTY, null);
    		frame.putClientProperty(TABBED_PANE_PROPERTY, null);
    	} else {
    		super.closeFrame(frame);
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
        	if (child instanceof DockingTabbedPane) {
        		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
        		for (JInternalFrame internalFrame: tabbedPane.getFrames()) {
        			try {
						internalFrame.setMaximum(true);
					} catch (PropertyVetoException e) {
						//ignore
					}
        		}
        	}
        	super.maximizeFrame(frame);
    	}
    }

	public void iconifyFrame(JInternalFrame frame) {
    	Component child = getChild(frame);
    	if (child instanceof DockingTabbedPane) {
    		JDesktopPane desktop = (JDesktopPane)child.getParent();
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
    		int tabIndex = tabbedPane.getTabIndex(frame);
    		frame.putClientProperty(TABBED_PANE_PROPERTY, tabbedPane);
    		tabbedPane.removeTabAt(tabIndex);
    		handleRemoveLastTab(tabbedPane);
    		JDesktopIcon icon = frame.getDesktopIcon();
    		if (desktop != null) {
    			desktop.add(icon);
    		}
    	} else if (frame.getClientProperty(TABBED_PANE_PROPERTY) != null) {
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)frame.getClientProperty(TABBED_PANE_PROPERTY);
            JDesktopPane desktop = frame.getDesktopPane();
    		DockLayout layout = (DockLayout)desktop.getLayout();
            super.iconifyFrame(frame);
    		layout.replaceLayoutComponent(frame, tabbedPane);
    		desktop.remove(frame);
    		desktop.add(tabbedPane);
    		frame.setVisible(true);
    		tabbedPane.setVisible(false);
    		tabbedPane.putClientProperty(INTERNAL_FRAME_PROPERTY, null);
    	} else {
    		super.iconifyFrame(frame);
    	}
    }

    public void deiconifyFrame(JInternalFrame frame) {
    	if (frame.getClientProperty(TABBED_PANE_PROPERTY) != null) {
            JDesktopIcon icon = frame.getDesktopIcon();
            Container parent = icon.getParent();
            if (parent != null) {
            	parent.remove(icon);
            }
            frame.setVisible(true);
            DockingTabbedPane tabbedPane = (DockingTabbedPane)frame.getClientProperty(TABBED_PANE_PROPERTY);
            handleAddTab(tabbedPane);
            frame.putClientProperty(TABBED_PANE_PROPERTY, null);
            tabbedPane.addTab(frame);
            if (frame.isMaximum()) {
            	DockLayout layout = (DockLayout)parent.getLayout();
            	layout.maximizeLayoutComponent(tabbedPane);
            	((JLayeredPane)parent).moveToFront(tabbedPane);
            }
        	handleRemoveLastTab(tabbedPane); //In certain circumstances the added tab was the first
            setSelected(frame);    		
    	} else {
            JDesktopIcon icon = frame.getDesktopIcon();
            Container parent = icon.getParent();
            JInternalFrame maximizedFrame = null;
            for (Component component: parent.getComponents()) {
            	maximizedFrame = getInternalFrame(component);
            	if (maximizedFrame != null && maximizedFrame.isVisible() && maximizedFrame.isMaximum()) {
            		break;
            	} else {
            		maximizedFrame = null;
            	}
            }
    		if (maximizedFrame != null) {
                if (parent != null) {
                	parent.remove(icon);
                }
                frame.setVisible(true);
				try {
					frame.setMaximum(true);
				} catch (PropertyVetoException e) {
					//ignore
				}
                Component child = getChild(maximizedFrame);
                if (child instanceof DockingTabbedPane) {
    				((DockingTabbedPane)child).addTab(frame);
    			} else {
    				addTab(maximizedFrame, 1, frame);
    			}
				maximizeFrame(frame);
    		} else {
    			super.deiconifyFrame(frame);
    		}
    	}
    }
    
    public void moveFrame(JComponent frame, int newX, int newY) {
    	Component child = getChild(frame);
    	if (child instanceof DockingTabbedPane && frame instanceof JInternalFrame) {
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
    		JInternalFrame internalFrame = (JInternalFrame)frame;
    		tabbedPane.removeTab(internalFrame);
    		child.getParent().add(frame);
        	super.moveFrame(frame, newX, newY);
    		handleRemoveLastTab(tabbedPane);
    	} else {
    		super.moveFrame(frame, newX, newY);
    	}
    }
    
    protected transient int tabIndex = -1;
    
    protected void calculateDragBounds(Component dragDestination, int x, int y) {
    	if (dragDestination instanceof JInternalFrame) {
    		JInternalFrame frame = (JInternalFrame)dragDestination;
    		int titleHeight = frame.getContentPane().getLocationOnScreen().y - frame.getLocationOnScreen().y;
    		if (y >= frame.getY() && y <= frame.getY() + titleHeight) {
    			frame.getBounds(dragBounds);
    			dragBounds.height = titleHeight;
    			dragBounds.width /= 2;
    			tabIndex = 0;
    			if (x > frame.getX() + frame.getWidth() / 2) {
    				dragBounds.x = frame.getX() + frame.getWidth() / 2;
        			tabIndex = 1;
    			}
    		} else {
    			tabIndex = -1;
    			super.calculateDragBounds(dragDestination, x, y);
    		}
    	} else if (dragDestination instanceof JTabbedPane) {
    		JTabbedPane tabbedPane = (JTabbedPane)dragDestination;
    		Rectangle bounds = null;
    		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
    			bounds = tabbedPane.getUI().getTabBounds(tabbedPane, i);
    			if (bounds.contains(x - tabbedPane.getX(), y - tabbedPane.getY())) {
    				tabIndex = i;
    				dragBounds.setBounds(bounds);
    				dragBounds.translate(tabbedPane.getX(), tabbedPane.getY());
    				return;
    			}
    		}
    		if (bounds != null) {
    			bounds.x += bounds.width;
    			bounds.width = tabbedPane.getWidth() - bounds.x;
    			if (bounds.contains(x - tabbedPane.getX(), y - tabbedPane.getY())) {
    				tabIndex = tabbedPane.getTabCount();
    				dragBounds.setBounds(bounds);
    				dragBounds.translate(tabbedPane.getX(), tabbedPane.getY());
    				return;
    			}    			
    		}
    		tabIndex = -1;
    		JInternalFrame frame = getInternalFrame(tabbedPane);
    		if (frame != null && frame.isMaximum()) {
    			tabbedPane.getBounds(dragBounds);
    			dragBounds.width = 0;
    			dragBounds.height = 0;
    		} else {
    			super.calculateDragBounds(dragDestination, x, y);
    		}
    	} else {
    		tabIndex = -1;
    		super.calculateDragBounds(dragDestination, x, y);
    	}
    }
    
    public void endDraggingFrame(JComponent frame) {
    	if (tabIndex > -1) {
        	drawBounds(frame);
        	JDesktopPane desktop = getDesktopPane(frame);
        	Component component = desktop.getComponentAt(dragBounds.x, dragBounds.y);
        	if (component != frame && frame instanceof JInternalFrame) {
        		removeFrame(frame);
        		//component may have changed due to the call of removeFrame
        		component = desktop.getComponentAt(dragBounds.x, dragBounds.y);
        		addTab(component, tabIndex, (JInternalFrame)frame);
        	}
    		tabIndex = -1;
    		if (frame instanceof JInternalFrame) {
    			setSelected((JInternalFrame)frame);
    		}
    	} else if (!(frame instanceof JInternalFrame) || !((JInternalFrame)frame).isMaximum()) {
    		super.endDraggingFrame(frame);
    	}
    }
    
    private void removeFrame(JComponent frame) {
		Component child = getChild(frame);
		if (child instanceof DockingTabbedPane && frame instanceof JInternalFrame) {
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
    		JInternalFrame internalFrame = (JInternalFrame)frame;
    		tabbedPane.removeTab(internalFrame);
    		handleRemoveLastTab(tabbedPane);
		} else {
			child.getParent().remove(child);
		}    	
    }
    
    void addTab(Component tabbedPane, int index, JInternalFrame frame) {
    	DockingTabbedPane dockingPane;
    	if (tabbedPane instanceof DockingTabbedPane) {
    		dockingPane = (DockingTabbedPane)tabbedPane;
    	} else if (tabbedPane instanceof JInternalFrame) {
    		dockingPane = new DockingTabbedPane();
    		dockingPane.putClientProperty(INTERNAL_FRAME_PROPERTY, tabbedPane);
    		handleAddTab(dockingPane);
    	} else {
    		throw new IllegalArgumentException("tabbedPane must either be of type DockingTabbedPane or JInternalFrame");
    	}
		dockingPane.addTab(frame, index);
    }

    protected void setSelected(JInternalFrame frame) {
    	super.setSelected(frame);
    	Component child = getChild(frame);
    	if (child instanceof JTabbedPane) {
    		DockingTabbedPane tabbedPane = (DockingTabbedPane)child;
    		int index = tabbedPane.getTabIndex(frame);
    		tabbedPane.setSelectedIndex(index);
    	}
    }
    
    protected void handleAddTab(DockingTabbedPane tabbedPane) {
        if (tabbedPane.getClientProperty(INTERNAL_FRAME_PROPERTY) != null) {
        	JInternalFrame internalFrame = (JInternalFrame)tabbedPane.getClientProperty(INTERNAL_FRAME_PROPERTY);
        	internalFrame.putClientProperty(TABBED_PANE_PROPERTY, null);
        	JDesktopPane desktop = (JDesktopPane)internalFrame.getParent();
        	DockLayout dockLayout = (DockLayout)desktop.getLayout();
        	dockLayout.replaceLayoutComponent(internalFrame, tabbedPane);
        	desktop.remove(internalFrame);
        	desktop.add(tabbedPane);
        	tabbedPane.addTab(internalFrame);
        	desktop.moveToFront(tabbedPane);
        }
    	tabbedPane.setVisible(true);
    }
    
    protected void handleRemoveLastTab(JTabbedPane tabbedPane) {
    	if (tabbedPane.getTabCount() == 1) {
    		JDesktopPane desktop = (JDesktopPane)getChild(tabbedPane).getParent();
    		if (desktop == null) {
    			return;
    		}
    		JInternalFrame frame = getInternalFrame(tabbedPane, 0);
    		frame.putClientProperty(TABBED_PANE_PROPERTY, tabbedPane);
    		tabbedPane.putClientProperty(INTERNAL_FRAME_PROPERTY, frame);
    		tabbedPane.removeTabAt(0);
    		DockLayout dockLayout = (DockLayout)desktop.getLayout();
    		dockLayout.replaceLayoutComponent(tabbedPane, frame);
    		desktop.remove(tabbedPane);
    		desktop.add(frame);
    		desktop.moveToFront(frame);
    	}
    }
    
    protected JInternalFrame getInternalFrame(JTabbedPane tabbedPane, int tabIndex) {
    	return getInternalFrame(tabbedPane.getComponentAt(tabIndex));
    }
    
    private JInternalFrame getInternalFrame(Component component) {
    	if (component instanceof JInternalFrame) {
    		return (JInternalFrame)component;
    	}
    	if (component instanceof Container) {
    		for (Component c: ((Container)component).getComponents()) {
    			JInternalFrame internalFrame = getInternalFrame(c);
    			if (internalFrame != null) {
    				return internalFrame;
    			}
    		}
    	}
    	return null;
    }
}
