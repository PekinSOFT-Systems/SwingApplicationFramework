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
 * Class      :   IconDockLayout.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 9:59:51 AM
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame.JDesktopIcon;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class IconDockLayout extends DockLayout {

	private List<JDesktopIcon> icons = new ArrayList<JDesktopIcon>();

	@Override
	public void addLayoutComponent(Component component, Object constraints) {
		if (component instanceof JDesktopIcon) {
			icons.add((JDesktopIcon)component);
		} else {
			super.addLayoutComponent(component, constraints);
		}
	}

	@Override
	public void removeLayoutComponent(Component component) {
		if (component instanceof JDesktopIcon) {
			icons.remove(component);
		} else {
			super.removeLayoutComponent(component);
		}
	}
	
	@Override
	public void layoutContainer(Container parent) {
		Insets insets = parent.getInsets();
		int iconWidth = getPreferredIconWidth();
		int iconHeight = getPreferredIconHeight();
		int iconX = 0;
		int iconY = parent.getHeight() - iconHeight;
		layout(insets.left, insets.top, parent.getWidth() - insets.left - insets.right, parent.getHeight() - insets.top - insets.bottom - iconHeight);
		for (JDesktopIcon icon: icons) {
			icon.setVisible(true);
			int width = icon.getPreferredSize().width;
			if (iconWidth > parent.getWidth()) {
				width = parent.getWidth() / icons.size();
			}
			icon.setBounds(iconX, iconY, width, iconHeight);
			iconX += width;
		}
		for (Component component: parent.getComponents()) {
			if (!(component instanceof JDesktopIcon)
			    && (component.getY() + component.getHeight() > parent.getHeight() - insets.bottom - iconHeight)) {
				component.setBounds(component.getX(), component.getY(), component.getWidth(),
						            parent.getHeight() - insets.bottom - iconHeight - component.getY());
			}
		}
	}
	
	private int getPreferredIconWidth() {
		int width = 0;
		for (JDesktopIcon icon: icons) {
			width = Math.max(width, icon.getPreferredSize().width);
		}
		return width;
	}

	private int getPreferredIconHeight() {
		int height = 0;
		for (JDesktopIcon icon: icons) {
			height = Math.max(height, icon.getPreferredSize().height);
		}
		return height;
	}
}
