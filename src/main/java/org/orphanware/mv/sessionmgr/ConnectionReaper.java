/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orphanware.mv.sessionmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 class ConnectionReaper extends Thread {

    private MVSessionMgr pool;
    private final long delay = 15000;
    private Logger logger = LoggerFactory.getLogger(ConnectionReaper.class);
    
    ConnectionReaper(MVSessionMgr mgr) {
        logger.debug("instantiating new reaper thread");
        setName("ConnectionReaper");
        this.pool = mgr;
        
    }

    @Override
    public void run() {
        while (true) {
            try {
                
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                }
                pool.reapConnections();
        
            }catch(Exception e) {
                logger.info("ConnectionReaper Failed!");
            }
        }
    }
}