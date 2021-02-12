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
 *  Class      :   MnemonicText.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 8:58:05 AM
 *  Modified   :   Feb 11, 2021
 *  
 *  Purpose:     See class JavaDoc comment.
 *  
 *  Revision History:
 *  
 *  WHEN          BY                   REASON
 *  ------------  -------------------  -----------------------------------------
 *  ??? ??, 2006  Hans Muller          Initial creation.
 *  Feb 11, 2021  Sean Carrick         Updated to Java 11.
 * *****************************************************************************
 */

package org.jdesktop.application;

import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * An internal helper class that configures the text and mnemonic properties for
 * instances of <code>AbstractButton</code>, <code>JLabel</code>, and <code>
 * j0avax.swing.Action</code>. It's used like this:
 * ```java
 * MnemonicText.configure(myButton, "Save &As")
 * ```
 * <p>
 * The configure method unconditionally sets three properties on the target 
 * object:</p>
 * <ul>
 * <li>the label text, "Save As"</li>
 * <li>the mnemonic key code, VK_A</li>
 * <li>the index of the mnemonic character, 5</li>
 * </ul>
 * <p>
 * If the mnemonic marker character isn't present, then the second two
 * properties are cleared to VK_UNDEFINED (0) and -1 respectively.</p>
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
class MnemonicText {

    private MnemonicText() {
    } // not used

    /**
     * Provides a means of setting up the accelerator character for an
     * <code>Action</code>, <code>JButton</code>, <code>JLabel</code>, <code>
     * JMenu</code>, and/or <code>JMenuItem</code>.
     * 
     * @param target the target of the accelerator key, i.e., action, button,
     *          label, menu, and/or menu item
     * @param markedText the text (or caption) that has a mnemonic marker 
     *          included
     */
    public static void configure(Object target, String markedText) {
        String text = markedText;
        int mnemonicIndex = -1;
        int mnemonicKey = KeyEvent.VK_UNDEFINED;
        // T B D: mnemonic marker char should be an application resource
        // Handling Hans Mullers' T B D here. Feb 11, 2021 by Sean Carrick.
        ResourceMap rm = new ResourceMap(null, MnemonicText.class.getClassLoader(),
                "org.jdesktop.application.resources.Application.properties");
        char mnemonicChar = (char) rm.getObject("Application.mnemonic.char", 
                char.class);
        int markerIndex = mnemonicMarkerIndex(markedText, mnemonicChar);
        if (markerIndex == -1) {
            markerIndex = mnemonicMarkerIndex(markedText, '_');
        }
        if (markerIndex != -1) {
            text = text.substring(0, markerIndex) + text.substring(markerIndex 
                    + 1);
            mnemonicIndex = markerIndex;
            CharacterIterator sci = new StringCharacterIterator(markedText, 
                    markerIndex);
            mnemonicKey = mnemonicKey(sci.next());
        }
        /* Updated this if...else if...else block to include the ability to set
         * a mnemonic accelerator on JMenu and JMenuItem, as well as the original
         * three components.
         *                                         -> Sean Carrick, Feb 11, 2021
        */
        if (target instanceof javax.swing.Action) {
            configureAction((javax.swing.Action) target, text, mnemonicKey, 
                    mnemonicIndex);
        } else if (target instanceof AbstractButton) {
            configureButton((AbstractButton) target, text, mnemonicKey, 
                    mnemonicIndex);
        } else if (target instanceof JLabel) {
            configureLabel((JLabel) target, text, mnemonicKey, mnemonicIndex);
        }else if (target instanceof JMenu) {
            configureMenu((JMenu) target, text, mnemonicKey, mnemonicIndex);
        } else if (target instanceof JMenuItem) {
            configureMenuItem((JMenuItem) target, text, mnemonicKey, 
                    mnemonicIndex);
        } else {
            throw new IllegalArgumentException("unrecognized target type " 
                    + target);
        }
    }

    private static int mnemonicMarkerIndex(String s, char marker) {
        if ((s == null) || (s.length() < 2)) {
            return -1;
        }
        CharacterIterator sci = new StringCharacterIterator(s);
        int i = 0;
        while (i != -1) {
            i = s.indexOf(marker, i);
            if (i != -1) {
                sci.setIndex(i);
                char c1 = sci.previous();
                sci.setIndex(i);
                char c2 = sci.next();
                boolean isQuote = (c1 == '\'') && (c2 == '\'');
                boolean isSpace = Character.isWhitespace(c2);
                if (!isQuote && !isSpace && (c2 != CharacterIterator.DONE)) {
                    return i;
                }
            }
            if (i != -1) {
                i += 1;
            }
        }
        return -1;
    }

    /* A general purpose way to map from a char to a KeyCode is needed.  An 
     * AWT RFE has been filed: 
     * http://bt2ws.central.sun.com/CrPrint?id=6559449
     * CR 6559449 java/classes_awt Support for converting from char to KeyEvent VK_ keycode
     */
    /*
    ****************************************************************************
    * I have made no changes to this, as I tried to look up the RFE listed     *
    * above, but:                                                              *
    *                                                                          *
    *       A. That site, obviously, no longer exists. It is not one that      *
    *          Oracle has recreated within their own servers.                  *
    *       B. The WayBackMachine (http://web.archive.com) has never crawled   *
    *          and archived that site, so I cannot get to it there.            *
    *                                                                          *
    * Therefore, with no way to see if that RFE was ever acted upon and added  *
    * to the JDK, I am just leaving this fix here and not risking breaking the *
    * API.                                                                     *
    *                                                                          *
    * If anyone knows about this RFE and if it was encorporated into the JDK,  *
    * please feel free to modify this class to incorporate the updated         *
    * functionality.                                                           *
    *                                            -> Sean Carrick, Feb 11, 2021 *
    ****************************************************************************
    */
    private static int mnemonicKey(char c) {
        int vk = (int) c;
        if ((vk >= 'a') && (vk <= 'z')) {
            vk -= ('a' - 'A');
        }
        return vk;
    }

    /* This javax.swing.Action constants is only 
     * defined in Mustang (1.6), see 
     * http://download.java.net/jdk6/docs/api/javax/swing/Action.html
     */
    /*
    ****************************************************************************
    * The constant definition below is no longer required, as the constant is  *
    * defined in the javax.swing.Action class, as noted above, since JDK6.     *
    * I have left the definition for historical purposes, but have commented   *
    * it out, and changed the reference to it in the `configureAction` method  *
    * to point to the constant defined in javax.swing.Action.                  *
    *                                                                          *
    *                                            -> Sean Carrick, Feb 11, 2021 *
    ****************************************************************************
    */
//    private static final String DISPLAYED_MNEMONIC_INDEX_KEY 
//                                           = "SwingDisplayedMnemonicIndexKey";

    private static void configureAction(javax.swing.Action target, String text, 
            int key, int index) {
        target.putValue(javax.swing.Action.NAME, text);
        if (key != KeyEvent.VK_UNDEFINED) {
            target.putValue(javax.swing.Action.MNEMONIC_KEY, key);
        }
        if (index != -1) {
            target.putValue(javax.swing.Action.DISPLAYED_MNEMONIC_INDEX_KEY, 
                    index);
        }
    }

    private static void configureButton(AbstractButton target, String text, 
            int key, int index) {
        target.setText(text);
        if (key != KeyEvent.VK_UNDEFINED) {
            target.setMnemonic(key);
        }
        if (index != -1) {
            target.setDisplayedMnemonicIndex(index);
        }
    }

    private static void configureLabel(JLabel target, String text, int key, 
            int index) {
        target.setText(text);
        if (key != KeyEvent.VK_UNDEFINED) {
            target.setDisplayedMnemonic(key);
        }
        if (index != -1) {
            target.setDisplayedMnemonicIndex(index);
        }
    }
    
    private static void configureMenu(JMenu target, String text, int key, 
            int index) {
        target.setText(text);
        if (key != KeyEvent.VK_UNDEFINED) {
            target.setMnemonic(key);
        }
        if (index != -1) {
            target.setDisplayedMnemonicIndex(index);
        }
    }
    
    private static void configureMenuItem(JMenuItem target, String text, int key, int index) {
        target.setText(text);
        if (key != KeyEvent.VK_UNDEFINED) {
            target.setMnemonic(key);
        }
        if (index != -1) {
            target.setDisplayedMnemonicIndex(index);
        }
    }
}
