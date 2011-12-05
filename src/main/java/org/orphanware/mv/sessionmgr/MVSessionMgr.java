/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orphanware.mv.sessionmgr;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author arash
 */
public class MVSessionMgr implements MVSessionMgrMBean {

    private List<MVConnection> connections;
    private String url, user, password, account, accountPassword;
    private int poolsize = 1;
    private ConnectionReaper reaper;
    private PoolFiller filler;
    private MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
    private Logger logger = LoggerFactory.getLogger(MVSessionMgr.class);
    //singleton attributes
    private static MVSessionMgr instance;
    private static Object syncObj = new Object();

    @Override
    public int getActiveConnCount() {

        return this.connections.size();
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getPoolsize() {
        return poolsize;
    }

    public void setPoolsize(int poolsize) {
        this.poolsize = poolsize;
    }

    private MVSessionMgr() {
    }

    public static MVSessionMgr getInstance() throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException, MalformedObjectNameException {

        synchronized (syncObj) {

            if (instance != null) {
                return instance;
            }


            instance = new MVSessionMgr();
            instance.init();
            return instance;
        }
    }

    private void init() throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException, MalformedObjectNameException {

        logger.debug("building new MVSessionMgr...");

        this.connections = new ArrayList<MVConnection>(poolsize);

        reaper = new ConnectionReaper(this);
        reaper.start();

        filler = new PoolFiller(this);
        filler.start();

        mserver.registerMBean(this, new ObjectName("org.orphanware.mv.jdbc:type=index"));
    }

    public void reapConnections() {

        synchronized (this.connections) {
            
            logger.info(Thread.currentThread().getName() + " reaping mv connections (curr pool size " + this.connections.size() + "...");


            Iterator it = this.connections.iterator();


            while (it.hasNext()) {
                
                MVConnection tmpConn = (MVConnection) it.next();
                long stale = System.currentTimeMillis() - tmpConn.getMvCallTimeOut();


                if ((tmpConn.inUse()) && (stale > tmpConn.getLastUse())) {

                    logger.error(Thread.currentThread().getName() + " stale connection found. killing connection");
                    tmpConn.killConnection();
                    it.remove();
                    
                }

            }

            it = this.connections.iterator();

            while (it.hasNext()) {

                MVConnection tmpConn = (MVConnection) it.next();

                if ((tmpConn.pingTest() == false)) {

                    logger.error(Thread.currentThread().getName() + " ping test failed. killing connection");
                    tmpConn.killConnection();
                    it.remove(); 
                }

            }
        }


    }

    public void fillPool() {

        if (this.url == null) {
            return;
        }

        synchronized (this.connections) {
            logger.debug(Thread.currentThread().getName() + " has taken the pool");
            if (this.connections.size() < poolsize) {

                int diff = poolsize - this.connections.size();
                for (int i = 0; i < diff; i++) {

                    try {
                        logger.debug(Thread.currentThread().getName() + " logging into account " + account);
                        this.connections.add(new MVConnection(this));
                        logger.info(Thread.currentThread().getName() + " added connection to pool (" + this.connections.size() + ")..");

                    } catch (SQLException ex) {
                        //no need to do anything since connection will handle
                    }

                }
            }
            logger.debug(Thread.currentThread().getName() + " has released the pool");

        }


    }

    @Override
    public void closeAllConnections() {

        Iterator it = this.connections.iterator();

        while (it.hasNext()) {

            MVConnection tmpConn = (MVConnection) it.next();
            tmpConn.killConnection();

        }
        this.connections = null;
        this.connections = new ArrayList<MVConnection>(poolsize);

    }

    public Connection getConnection() throws SQLException {
        logger.debug(Thread.currentThread().getName() + " is requesting a connection b4 sync");
        synchronized (this.connections) {
            logger.debug(Thread.currentThread().getName() + " is requesting a connection after sync");
            for (MVConnection tmpConn : this.connections) {

                if (tmpConn.lease()) {
                    return tmpConn;
                }



            }

            if (this.connections.size() == poolsize) {

                try {
                    this.connections.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }


                return getConnection();
            }

            MVConnection conn = new MVConnection(this);

            conn.lease();
            this.connections.add(conn);

            return conn;
        }


    }

    public void returnConnection(MVConnection conn) {

        synchronized (this.connections) {
            conn.expireLease();
            this.connections.notify();
        }

    }

    public void removeConnection(MVConnection conn) {

        synchronized (this.connections) {

            if (this.connections.contains(conn)) {
                this.connections.remove(conn);
            }
            this.connections.notify();
        }

    }
}
