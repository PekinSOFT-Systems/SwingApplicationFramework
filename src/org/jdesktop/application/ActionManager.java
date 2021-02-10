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
 * Project    :   SwingApplicationFramework
 * Class      :   ActionManager.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 30, 2021 @ 8:58:39 AM
 * Modified   :   Jan 30, 2021
 *  
 * Purpose:     See class JavaDoc
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 30, 2021  Sean Carrick        Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application;

import org.jdesktop.application.utils.Logger;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * The application's <tt>ActionManager</tt> provides read-only cached access to
 * `ActionMap`s that contain one entry for each method marked with the
 * <tt>@Action</tt> annotation in a class.
 * 
 * @see ApplicationContext#getActionMap(Object)
 * @see ApplicationActionMap
 * @see ApplicationAction
 * 
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class ActionManager {
    
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());
    private final ApplicationContext context;
    private final WeakHashMap<Object, WeakReference<ApplicationActionMap>> actionMaps;
    private ApplicationActionMap globalActionMap = null;
    
    /**
     * Creates a default instance of the ActionManager class.
     * 
     * @param context the <tt>ApplicationContext</tt> to which this `
     *          ActionManager} belongs
     */
    public ActionManager (ApplicationContext context) {
        this.context = context;
        actionMaps = null;
    }

    ApplicationActionMap getActionMap() {
        // TODO: Implement method functionality
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ApplicationActionMap getActionMap(Class actionsClass, Object actionsObject) {
        // TODO: Implement method functionality
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
