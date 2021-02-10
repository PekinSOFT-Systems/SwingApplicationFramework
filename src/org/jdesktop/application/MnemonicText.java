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
 * Class Name: MnemonicText.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Jan 18 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 01/18/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import javax.swing.AbstractButton;
import javax.swing.JLabel;

/**
 * An internal helper class that configures the text and mnemonic properties for
 * instances of AbstractButton, JLabel, and javax.swing.Action. It is used like
 * this:
 * <pre>
 * MnemonicText.configure(myButton, "Save &amp;As");
 * </pre> The configure method unconditionally sets three properties on the
 * target object:
 * <ul>
 * <li>the label text, "Save As"</li>
 * <li>the mnemonic key code, VK_A</li>
 * <li>the index of the mnemonic character, 5</li>
 * </ul>
 * If the mnemonic marker character is not present, then the second two
 * properties are cleared to VK_UNDEFINED (0) and -1 respectively.
 *
 * @author Sean Carrick
 */
public class MnemonicText {

    private MnemonicText() {
    } // Not used

    public static void configure(Object target, String markedText) {
        String text = markedText;
        int mnemonicIndex = -1;
        int mnemonicKey = KeyEvent.VK_UNDEFINED;
        /* Taking care of TBD note in the original version of the Swing 
         * Application Framework and making the Mnemonic Character an 
         * application resource.
         *
         * TBD handled by Sean Carrick <sean at pekinsoft dot com>
         * TBD handled on January 18, 2021
         */
        ResourceMap rm = new ResourceMap(null,
                MnemonicText.class.getClassLoader(),
                "com.pekinsoft.desktop.resources.Application.properties");
        char mnemonicChar = (char) rm.getObject("Application.mnemonic.char",
                char.class);
        int markerIndex = mnemonicMarkerIndex(markedText, mnemonicChar);
        if (markerIndex != -1) {
            text = text.substring(0, markerIndex)
                    + text.substring(markerIndex + 1);
            mnemonicIndex = markerIndex;
            CharacterIterator sci = new StringCharacterIterator(markedText,
                    markerIndex);
            mnemonicKey = mnemonicKey(sci.next());
        }

        if (target instanceof javax.swing.Action) {
            configureAction((javax.swing.Action) target, text, mnemonicKey,
                    mnemonicIndex);
        } else if (target instanceof AbstractButton) {
            configureButton((AbstractButton) target, text, mnemonicKey,
                    mnemonicIndex);
        } else if (target instanceof JLabel) {
            configureLabel((JLabel) target, text, mnemonicKey, mnemonicIndex);
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

                if (!isQuote && !isSpace && (c2 != sci.DONE)) {
                    return i;
                }
            }

            if (i != -1) {
                i += 1;
            }
        }

        return i;
    }
    
    /* A general-purpose way to map from a char to a KeyCode is needed. An AWT
     * RFE has been filed:
     * http://bt2ws.central.sun.com/CrPrint?id=6559449
     * CR 6559449 java/classes_awt Support for converting from char to KeyEvent
     * VK_keycode
     */
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * The note above is from the original Swing Application Framework API. I 
     * just did a search on this feature in Google and DuckDuckGo and found no
     * results stating that the RFE had been acted upon, and the WayBack Machine
     * (http://web.archive.org ) has not archived that URL, nor any of the pages
     * under http://bt2ws.central.sun.com/ so we will use the original author's
     * work-around.
     *
     * Sean Carrick (Adapting Author) <sean at pekinsoft dot com>
     * January 18, 2021
     */
    private static int mnemonicKey(char c) {
        int vk = (int) c;
        if ((vk >= 'a') && (vk <= 'z')) {
            vk -= ('a' - 'A');
        }
        
        return vk;
    }
    
    /* This javax.swing.Action constant is only defined in Mustang (1.6), see
     * http://download.java.net/jdk6/docs/api/javax/swing/Action.html
     */
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * The note above is from the original Swing Application Framework API.
     * java.net is defunct now and the WayBack Machine last successfully archived
     * the link on December 7, 2006, and has it at the following URL:
     * http://web.archive.org/web/20061207183717/http://download.java.net/jdk6/docs/api/javax/swing/Action.html
     *
     * However, a lot has changed since this library was originally written, and
     * The referenced constant is included in the javax.swing.Action of today.
     * Therefore, I have commented out the original author's constant declaration
     * and modified the code in the body of the message to reflect the updates
     * to the Java language. I am leaving the original constant declaration in
     * place, but commented out, for historical purposes.
     *
     * Sean Carrick (Adapting Author) <sean at pekinsoft dot com>
     * January 18, 2021
     */
    // private static final String DISPLAYED_MNEMONIC_INDEX_KEY 
    //+        = "SwingDisplayedMnemonicIndexKey";
    
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
        
        if ( index != -1) {
            target.setDisplayedMnemonicIndex(index);
        }
    }
}
