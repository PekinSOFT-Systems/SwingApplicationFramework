/*
 * Copyright (C) 2006-2021 PekinSOFT Systems
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
 * *****************************************************************************
 * Class Name: ApplicationContext.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Jan 16 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 01/16/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package com.pekinsoft.desktop.application;

import com.pekinsoft.desktop.beans.AbstractBean;
import com.pekinsoft.desktop.storage.LocalStorage;
import com.pekinsoft.desktop.task.TaskService;
import com.pekinsoft.desktop.utils.Logger;
import java.util.List;
import org.jdesktop.swingx.action.ActionManager;

/**
 * A singleton that manages shared objects, like actions, resources, and tasks,
 * for {@code Application}s.
 * <p>
 * {@link Application Application}s use the {@code ApplicationContext} singleton
 * to find global values and services. The majority of the Swing Application
 * Framework API can be accessed through {@code ApplicationContext}. The static
 * {@code getInstance} method returns the singleton. Typically it is only called
 * after the application has been {@link Application#launch launch}ed, however,
 * it is always safe to call {@code getInstance}.
 * 
 * @see Application
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 */
public class ApplicationContext extends AbstractBean {
    private static final Logger logger = Logger.getLogger(
            ApplicationContext.class.getName());
    private final List<TaskService> taskServices;
    private final List<TaskService> taskServicesReadOnly;
    private ResourceManager resourceManager;
    private ActionManager actionManager;
    private LocalStorage localStorage;
    private SessionStorage sessionStorage;
    private Application application = null;

    public LocalStorage getLocalStorage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
