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
 * Class      :   CloseButton.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 10:03:20 AM
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

package org.jdesktop.application.docking.title;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.swing.JInternalFrame;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class CloseButton extends AbstractTitleButton {

	public CloseButton(final JInternalFrame frame) {
		super("InternalFrame.closeIcon");
		addActionListener(new CloseAction(frame));
        setVisible(frame.isClosable());
        frame.addPropertyChangeListener("closable", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                setVisible(frame.isClosable());
            }
        });
	}
	
	private class CloseAction implements ActionListener {

		private JInternalFrame frame;
		
		public CloseAction(JInternalFrame internalFrame) {
			frame = internalFrame;
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				frame.setClosed(true);
			} catch (PropertyVetoException ignore) {
			}
		}
	}
}
