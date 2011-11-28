/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orphanware.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author arash
 */
public class StringHelper {
    
    
    public static String getStackTraceAsString(Exception e) {
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
        
    }
    
}
