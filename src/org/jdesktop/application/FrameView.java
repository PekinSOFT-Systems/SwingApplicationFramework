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
 *  Project    :   SwingApplicationFramework
 *  Class      :   FrameView.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 7:56:26 AM
 *  Modified   :   Feb 11, 2021
 *  
 *  Purpose:     See class JavaDoc comment.
 *  
 *  Revision History:
 *  
 *  WHEN          BY                   REASON
 *  ------------  -------------------  -----------------------------------------
 *  Feb 11, 2021  Sean Carrick         Initial creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JRootPane;

public class FrameView extends View {

    private static final Logger logger = Logger.getLogger(FrameView.class.getName());
    private JFrame frame = null;

    public FrameView(Application application) {
        super(application);
    }

    /**
     * Return the JFrame used to show this View
     *
     * <p>
     * This method may be called at any time; the JFrame is created lazily and
     * cached. For example:
     * ```java
     * &#064;Override 
     * protected void startup() {
     *     getFrame().setJMenuBar(createMenuBar());
     *     show(createMainPanel());
     * }
     * ```
     *
     * @return this application's main frame
     */
    public JFrame getFrame() {
        if (frame == null) {
            String title = getContext().getResourceMap().getString("Application"
                    + ".title");
            frame = new JFrame(title);
            frame.setName("mainFrame");
        }
        return frame;
    }

    /**
     * Sets the JFrame use to show this View
     * <p>
     * This method should be called from the startup method by a subclass that
     * wants to construct and initialize the main frame itself. Most
     * applications can rely on the fact that {code getFrame} lazily constructs
     * the main frame and initializes the {@code frame} property.
     * <p>
     * If the main frame property was already initialized, either implicitly
     * through a call to {@code getFrame} or by explicitly calling this method,
     * an IllegalStateException is thrown. If {@code frame} is null, an
     * IllegalArgumentException is thrown.</p>
     * <p>
     * This property is bound.</p>
     *
     *
     *
     * @param frame the new value of the frame property
     * @see #getFrame() 
     */
    public void setFrame(JFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("null JFrame");
        }
        if (this.frame != null) {
            throw new IllegalStateException("frame already set");
        }
        this.frame = frame;
        firePropertyChange("frame", null, this.frame);
    }

    public JRootPane getRootPane() {
        return getFrame().getRootPane();
    }
}
