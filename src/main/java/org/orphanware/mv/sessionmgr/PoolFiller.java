package org.orphanware.mv.sessionmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PoolFiller extends Thread {

    private MVSessionMgr pool;
    private final long delay = 60000;
    private Logger logger = LoggerFactory.getLogger(PoolFiller.class);
    
    PoolFiller(MVSessionMgr mgr) {
        logger.debug("instantiating new PoolFiller thread");
        setName("PoolFiller");
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
                pool.fillPool();
        
            }catch(Exception e) {
                logger.info("PoolFiller Failed!");
            }
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.info(getName() + " had died unexpectedly!");
    }
    
}