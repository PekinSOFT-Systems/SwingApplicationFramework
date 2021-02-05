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
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 30, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package com.pekinsoft.desktop.action;

import com.pekinsoft.desktop.application.ApplicationContext;
import com.pekinsoft.desktop.utils.Logger;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * The application's {@code ActionManager} provides read-only cached access to
 * {@code ActionMap}s that contain one entry for each method marked with the
 * {@code @Action} annotation in a class.
 * 
 * @see ApplicationContext#getActionMap(Object)
 * @see ApplicationActionMap
 * @see ApplicationAction
 * 
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class ActionManager {
    
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());
    private final ApplicationContext context;
    private final WeakHashMap<Object, WeakReference<ApplicationActionMap>> actionMaps;
    private ApplicationActionMap globalActionMap = null;
    
    /**
     * Creates a default instance of the ActionManager class.
     */
    public ActionManager () {
        
    }

}
