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
 *  Class      :   SysExits.java
 *  Author     :   Sean Carrick
 *  Created    :   Mar 8, 2020 @ 12:38:00 PM
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

package org.jdesktop.application.enums;

/**
 * PekinSOFT Systems System Exit Codes.  The following enumerations are used
 * throughout all PekinSOFT Systems programs for error codes and exit status 
 * codes.  We do this to enable our systems to report back to the operating 
 * system how they exited or what error they encountered while executing.  This 
 * allows software written and designed by PekinSOFT Systems to be automated 
 * through shell scripts and allow the shell scripts to test the exit status in 
 * order to take appropriate action.  The error codes used by Integrity 
 * Solutions programs are taken from the Linux C header file 'sysexits.h' to 
 * stay standardized with other Linux/Unix programs.  Other error codes are 
 * added as needed by PekinSOFT Systems software, if the error is not 
 * otherwise covered by the standard error codes that are already in sysexits.h.
 * <p>
 * PekinSOFT Systems is reusing these constants, directly from the `
 * sysexits.h} file, in compliance with the Berkeley License:</p>
 * <pre>
 * Copyright (c) 1987, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 *	&#0041;(#)sysexits.h	8.1 (Berkeley) 6/2/93
 * </pre>
 *
 * @author Original `sysexits.h` Authors (unlisted)
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 *
 * @version 1.05
 * @since 1.03
 */
public enum SysExits {
    /**
     * Normal application termination.
     */
    EX_OK ( 0 ),

    /**
     * User cancelled operation, which is usually at the logon screen.
     */
    EX_OPCANCEL ( 63 ),

    /**
     * Command-line usage error.  Should be used to exit program AFTER a short command-
     * line usage help is displayed ON STANDARD ERROR.
     */
    EX_USAGE ( 64 ),

    /**
     * Data format error.  May be used in conjunction with testing that valid data was
     * entered into a field on a form.  SHOULD *NOT* cause an exit to occur.
     */
    EX_DATAERR ( 65 ),

    /**
     * Cannot open input.  Should only be used if the input file does not exist.  If it is
     * a permissions problem, use the appropriate error code for permissions, EX_NOPERM.
     */
    EX_NOINPUT ( 66 ),

    /**
     * User unknown.  Should be used if the user attempting to log on does not have an
     * user account on the SQL server, or is otherwise unknown by the application.  By
     * exiting when the user does not have access, the SysAdmin will be able to check for
     * possible attacks on the system.
     *
     *   ~ NOTE WRITTEN ON 10/31/2008 ~
     * May not be used with built-in messaging within CAS.  This exit code should not be
     * needed for a while, as messaging will come much later.
     */
    EX_NOUSER ( 67 ),

    /**
     * Host name unknown.  Normally used for when the SQL server is not found.
     */
    EX_NOHOST ( 68 ),

    /**
     * Service unavailable.  Again, used with the SQL server, if host is found, but the
     * server service is not running.
     */
    EX_UNAVAILABLE ( 69 ),

    /**
     * Internal software error.  Could be for any internal error, including SQL errors
     * that are due to mistyped SQL statements or command, but not including SQL server
     * errors.
     */
    EX_SOFTWARE ( 70 ),

    /**
     * System error (e.g., can't fork).  Mainly used with threading issues.
     */
    EX_OSERR ( 71 ),

    /**
     * Critical OS file missing.
     */
    EX_OSFILE ( 72 ),

    /**
     * Can't create (user) output file.  Also may be used when unable to create a temporary
     * file.
     */
    EX_CANTCREAT ( 73 ),

    /**
     * Input/Output error.  Used when errors accessing a drive or system resource occur.
     */
    EX_IOERR ( 74 ),

    /**
     * Temporary failure; user is invited to retry.
     */
    EX_TEMPFAIL ( 75 ),

    /**
     * Remote error in protocol.  May be used if something goes wrong on the SQL server or
     * with the SQL server software itself, which causes our program to work unexpectedly
     * or not at all.  May also be used in the event of network collisions.
     */
    EX_PROTOCOL ( 76 ),

    /**
     * Permission denied.  Used for file system access, when the user does not have the
     * appropriate permissions for a folder, file or the database server.  SHOULD *NOT*
     * be used for an error opening a file or connection...use EX_NOINPUT for those errors.
     */
    EX_NOPERM ( 77 ),

    /**
     * Configuration error.  Used if our configuration files can't be found, created or
     * are corrupt.
     */
    EX_CONFIG ( 78 ),

    /**
     * Driver error.  Used if the attempt to load a driver for connecting to a
     * server fails.
     */
    EX_DRIVER ( 79 );

    /**
     * Used internally for manipulation of the data contained within this enum.
     */
    final Integer Status;

    /**
     * Allow this enum to convert itself to an Integer.  Must be called to use 
     * these enums as integers in tests or System.exit() calls.
     * 
     * @return int - the value of the enum.
     */
    public int toInt() {
        return Status;
    } // End of toInteger method.

    /**
     * All this enum to convert itself to a String.  Must be called to print
     * the current status in text to the console or to a file.
     */
    @Override
    public String toString() {
      // We will return the string value of the integer status.  For example,
      //+ if the status is 0, we will return "Normal".
      // Declare a variable to hold our return value.
      String retVal = null;

      // Figure out our status and set our return value.
      switch ( Status ) {
        case 0:   // OK
          retVal = "Normal";
          break;
        case 63:  // OPCANCEL
          retVal = "Operation Cancelled";
          break;
        case 64:  // USAGE
          retVal = "Command-line usage error";
          break;
        case 65:  // DATAERR
          retVal = "Data format error";
          break;
        case 66:  // NOINPUT
          retVal = "Cannot open input";
          break;
        case 67:  // NOUSER
          retVal = "Unknown user";
          break;
        case 68:  // NOHOST
          retVal = "Unknown host";
          break;
        case 69:  // UNAVAILABLE
          retVal = "Server unavailable";
          break;
        case 70:  // SOFTWARE
          retVal = "Internal software error";
          break;
        case 71:  // OSERR
          retVal = "System error (e.g., can't fork)";
          break;
        case 72:  // OSFILE
          retVal = "Critical OS file missing";
          break;
        case 73:  // CANTCREAT
          retVal = "Can't create (user) output file nor program object";
          break;
        case 74:  // IOERR
          retVal = "Input/Output error";
          break;
        case 75:  // TEMPFAIL
          retVal = "Temporary failure...please retry later";
          break;
        case 76:  // PROTOCOL
          retVal = "Remote error in protocol";
          break;
        case 77:  // NOPERM
          retVal = "Permission denied...check your permissions settings";
          break;
        case 78:  // CONFIG
          retVal = "Configuration error";
          break;
        case 79:  // DRIVER
            retVal = "Driver error";
            break;
        default:  // No other matches.
          retVal = "STATUS UNKNOWN!";
          break;
      } // End of switch...case to get our status.

      // Return our value.
      return ( retVal );
    }

    /**
     * Constructor.
     */
    SysExits ( int iStatus ) {
        this.Status = iStatus;
    } // End of constructor.

}
