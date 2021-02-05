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
 * Class      :   LocalStorage.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 31, 2021 @ 3:11:35 PM
 * Modified   :   Jan 31, 2021
 *  
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 31, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package com.pekinsoft.desktop.storage;

import com.pekinsoft.desktop.beans.AbstractBean;
import java.util.Map;

/**
 * Access to per application, per user, local file storage.
 *
 * @see ApplicationContext#getLocalStorage
 * @see SessionStorage
 * 
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class LocalStorage extends AbstractBean {

    /**
     * Creates a default instance of the LocalStorage class.
     */
    public LocalStorage () {
        
    }

    void save(Map<String, Object> stateMap, String fileName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
