/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.orphanware.mv.sessionmgr;

/**
 *
 * @author arash
 */
public interface MVSessionMgrMBean {

    public int getActiveConnCount();
    public void closeAllConnections();

}
