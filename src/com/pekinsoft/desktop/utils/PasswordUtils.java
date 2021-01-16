/*
 * Copyright (C) 2020 PekinSOFT Systems
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
 * *****************************************************************************
 *  Project    :   Northwind-Basic
 *  Class      :   PasswordUtils.java
 *  Author     :   Sean Carrick
 *  Created    :   Mar 8, 2020 @ 12:35:23 PM
 *  Modified   :   Mar 8, 2020
 *  
 *  Purpose:
 *  
 *  Revision History:
 *  
 *  WHEN          BY                  REASON
 *  ------------  ------------------- ------------------------------------------
 *  Mar 8, 2020  Sean Carrick        Initial creation.
 * *****************************************************************************
 */

package com.pekinsoft.desktop.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class PasswordUtils {

    //<editor-fold defaultstate="collapsed" desc="Private Member Fields">
    private static final SecureRandom RAND = new SecureRandom();
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 512;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor(s)">
    private PasswordUtils () {
        // Privatized constructor to keep class from being instantiated.
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public Static Methods">
    /**
     * Method provides a means of generating salt for password hashing to 
     * protect the password for hackers and allow for storage in a database.
     * <p>
     * Password-based encryption generates a cryptographic key using a user
     * password as a starting point. We irreversibly convert the password to a
     * fixed-length hash code using a one-way hash function, adding a second
     * random string as &quot;salt&quot;, to prevent hackers from performing
     * <em>dictionary attacks</em>, where a list of common passwords are mapped
     * to their hashed outputs -- if the hacker knows the hashing algorithm used
     * and can gain access to the database where the hashcodes are stored, they
     * can use their &quot;dictionary&quot; to map back to the original password
     * and gain access to those accounts.</p>
     * <p>
     * We provide this method to allow the calling application to generate salt.
     * 
     * @param length Number of bits of salt to generate.
     * @return       Optional&lt;String&gt; of the salt.
     */
    public static Optional<String> generateSalt(final int length) {
        if ( length < 1 ) {
            System.err.println("error in generateSalt :: length must be > 0");
            return Optional.empty();
        }
        
        byte[] salt = new byte[length];
        RAND.nextBytes(salt);
        
        return Optional.of(Base64.getEncoder().encodeToString(salt));
    }
    
    /**
     * This method actually performs the hashing of the provided password, using
     * the provided salt string. With these two ingredients, we will generate a
     * secure hash that can be stored in a database or password file and not be
     * able to be determined what the original password was.
     * <p>
     * For security's sake, we immediately convert the password to a character
     * array and the salt to a byte array. As soon as we are done with the 
     * provided strings, we set them up for garbage collection. Likewise, as
     * soon as we are done with the arrays, we place null characters in each
     * element, so as to keep the original password a secret.
     * 
     * @param password  String pulled from a password field on a logon dialog.
     * @param salt      String of randomly generated salt.
     * @return          A secure hash of the original password.
     */
    public static Optional<String> hashPassword(String password, String salt) {
        char[] chars = password.toCharArray();
        byte[] bytes = salt.getBytes();
        
        PBEKeySpec spec = new PBEKeySpec(chars, bytes, ITERATIONS, KEY_LENGTH);
        
        Arrays.fill(chars, Character.MIN_VALUE);
        
        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] securePassword = fac.generateSecret(spec).getEncoded();
            return Optional.of(Base64.getEncoder().encodeToString(securePassword));
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.err.println("Exception encountered in hashPassword()");
            return Optional.empty();
        } finally {
            spec.clearPassword();
        }
    }
    
    /**
     * This method allows the calling application to verify that a user entered
     * the correct password, by checking it against a known hash.
     * 
     * @param password The entered password.
     * @param key      The hash of the original password that was stored.
     * @param salt     The same random salt as originally used.
     * @return         Whether the passwords match or not.
     */
    public static boolean verifyPassword(String password, String key, String salt) {
        Optional<String> optEncrypted = hashPassword(password, salt);
        if (!optEncrypted.isPresent()) return false;
        return optEncrypted.get().equals(key);
    }
    //</editor-fold>

}
