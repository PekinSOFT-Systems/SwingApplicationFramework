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
 * Class      :   TitleLabel.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 10:05:29 AM
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

package com.pekinsoft.desktop.docking.title;

import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class TitleLabel extends JLabel {

	private JInternalFrame frame;
	
	public TitleLabel(JInternalFrame internalFrame) {
		frame = internalFrame;
		setIcon(frame.getFrameIcon());
		setText(frame.getTitle());
		DragListener dragListener = new DragListener();
		addMouseMotionListener(dragListener);
		addMouseListener(dragListener);
	}
	
	public Icon getIcon() {
		if (frame != null) {
			return frame.getFrameIcon();
		} else {
			return super.getIcon();
		}
	}
	
	public void setIcon(Icon icon) {
		if (frame != null) {
			frame.setFrameIcon(icon);
		}
		super.setIcon(icon);
	}
	
	public String getText() {
		if (frame != null) {
			return frame.getTitle();
		} else {
			return super.getText();
		}
	}
	
	public void setText(String text) {
		if (frame != null) {
			frame.setTitle(text);
		}
		super.setText(text);
	}
	
	public JTabbedPane getTabbedPane() {
		Container parent = getParent();
		while (parent != null && !(parent instanceof JTabbedPane)) {
			parent = parent.getParent();
		}
		return (JTabbedPane)parent;
	}
	
	private class DragListener extends MouseAdapter {

		private boolean dragging = false;
		
	    public void mouseDragged(MouseEvent e) {
	    	if (dragging) {
	    		frame.getDesktopPane().getDesktopManager().dragFrame(frame, e.getX(), e.getY());
	    	} else {
	    		frame.getDesktopPane().getDesktopManager().beginDraggingFrame(frame);
	    		dragging = true;
	    	}
	    }
	    
	    public void mouseReleased(MouseEvent e) {
	    	if (dragging) {
	    		frame.getDesktopPane().getDesktopManager().endDraggingFrame(frame);
	    		dragging = false;
	    	} else {
	    		JTabbedPane tabbedPane = getTabbedPane();
	    		int index = tabbedPane.indexOfTabComponent(TitleLabel.this.getParent());
	    		tabbedPane.setSelectedIndex(index);
	    	}
	    }
	}
}
