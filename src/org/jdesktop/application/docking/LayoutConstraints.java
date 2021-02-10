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
 * Class      :   LayoutConstraints.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 10:09:01 AM
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
import javax.swing.JInternalFrame;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class LayoutConstraints extends DockConstraints {

    public static TabConstraints tabBefore(JInternalFrame frame) {
        return new TabConstraints(frame, DockLocation.WEST);
    }

    public static TabConstraints tabAfter(JInternalFrame frame) {
        return new TabConstraints(frame, DockLocation.EAST);
    }
    
    protected LayoutConstraints(Component componentToDockAt, DockLocation dockLocation, float alignment) {
        super(componentToDockAt, dockLocation, alignment);
    }

    protected LayoutConstraints(Component componentToDockAt, DockLocation dockLocation) {
        super(componentToDockAt, dockLocation);
    }

    protected LayoutConstraints(Component componentToDockAt) {
        super(componentToDockAt);
    }

    static class TabConstraints extends LayoutConstraints {

        protected TabConstraints(JInternalFrame frame) {
            super(frame);
        }

        protected TabConstraints(JInternalFrame frame, DockLocation location) {
            super(frame, location);
        }
        
        public JInternalFrame getComponentToDockAt() {
            return (JInternalFrame)super.getComponentToDockAt();
        }
    }
}
