/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orphanware.mv.sessionmgr;

import org.orphanware.utils.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author arash
 */
public class ConnectionReaperBoss extends Thread{

    private final long delay = 30000;
    private Logger logger = LoggerFactory.getLogger(ConnectionReaper.class);
    private MVSessionMgr sessionMgr;
    private ConnectionReaper connReaper;
    
    
    public ConnectionReaperBoss(MVSessionMgr sessionMgr) {
        logger.debug("instantiating new ConnectionReaperBoss thread");
        setName("ConnectionReaperBoss");
        this.sessionMgr = sessionMgr;
        this.connReaper = sessionMgr.getConnectionReaper();
        
    }
    
    @Override
    public void run() {
        
        while(true) {
            
            try {
                
                try {
                 
                    sleep(delay);
                    
                } catch(Exception e) {
                    logger.info("ConnectionReaper Failed!");
                }
                //check to see if reaper is dead and if so bring it back to life
                logger.debug(getName() + " checking to see if ConnectionReaper is  alive...");
                Boolean isAlive = this.connReaper.isAlive();
                
                if(isAlive)
                    logger.debug("ConnectionReaper is alive and kicking!");
                else {
                    logger.info("ConnectionReaper is dead!  Instantiating new one!");
                    this.connReaper = null;
                    this.connReaper = new ConnectionReaper(sessionMgr);
                    this.connReaper.start();
                    this.sessionMgr.setConnectionReaper(this.connReaper);
                    
                }                    
                this.connReaper.interrupt();
            }catch (Exception e) {
                
                logger.debug(getName() + "has stopped! " + StringHelper.getStackTraceAsString(e));
            }
            
        }        
        
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.info(getName() + " had died unexpectedly!");
    }
    
    
    
    
}
