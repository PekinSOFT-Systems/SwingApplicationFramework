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
 * Class Name: FrameView.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Feb 06 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 02/37/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package org.jdesktop.application.view;

import org.jdesktop.application.Application;
import org.jdesktop.application.resources.ResourceMap;
import org.jdesktop.application.utils.Logger;
import javax.swing.JFrame;
import javax.swing.JRootPane;

/**
 * 
 *
 * @author Sean Carrick
 */
public class FrameView extends View {
    private static final Logger logger = Logger.getLogger(FrameView.class.getName());
    private JFrame frame = null;
    
    public FrameView(Application application) {
        super(application);
    }
    
    /**
     * Return the JFrame used to show this View.
     * <p>
     * This method may be called at any time; the JFrame is created lazily and 
     * cached. For example:</p>
     * <pre>
     * &#064;Override protected void startup() {
     *     getFrame().setJMenuBar(createMenuBar());
     *     show(createMainPanel());
     * }
     * </pre>
     * 
     * @return this application's main frame
     */
    public JFrame getFrame() {
        if (frame == null) {
            String title = getContext().getResourceMap().getString(
                    "Application.title");
            frame = new JFrame(title);
            frame.setName("mainFrame");
        }
        
        return frame;
    }
    
    /**
     * Sets the JFrame used to show this View.
     * <p>
     * This method should be called from the startup method by a subclass that
     * wants to construct and initialize the main frame itself. Most applications
     * can rely on the fact that {@code getFrame} lazily constructs the main 
     * frame and initialize the {@code frame} property.</p>
     * <p>
     * If the main frame property was already initialized, either implicitly 
     * through a call to {@code getFrame} or by explicitly calling this method,
     * an IllegalStateException is thrown. If {@code frame} is null, an
     * IllegalArguemntException is thrown.</p>
     * <p>
     * This property is bound.</p>
     * 
     * @param frame the new value of the frame property
     * @see #getFrame
     */
    public void setFrame(JFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("null frame");
        }
        if (this.frame != null) {
            throw new IllegalArgumentException("frame already set");
        }
        
        this.frame = frame;
        firePropertyChange("frame", null, this.frame);
    }
    
    /**
     * {@inheritDoc }
     * 
     * @return {@inheritDoc }
     */
    @Override
    public JRootPane getRootPane() {
        return getFrame().getRootPane();
    }
}
