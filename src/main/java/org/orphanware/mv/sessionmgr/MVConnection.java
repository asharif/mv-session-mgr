package org.orphanware.mv.sessionmgr;

import com.tigr.mvapi.exceptions.MVException;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.orphanware.utils.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author arash
 */
public class MVConnection{
    private String id;
    private MVSessionMgr pool;
    private com.tigr.mvapi.MVConnection conn;
    private boolean inuse;
    private long timestamp;
    private int timout = 3000;
    private int mvCallSubTimeOut = 15000;
    private Logger logger = LoggerFactory.getLogger(MVConnection.class);

    public int getMvCallTimeOut() {

        return this.mvCallSubTimeOut;
    }

    public MVConnection(MVSessionMgr pool) throws SQLException {

        this.id = UUID.randomUUID().toString();
        this.pool = pool;
        this.inuse = false;
        this.timestamp = 0;
        createNewMVConnection();
    }

    private void createNewMVConnection() throws SQLException {

        logger.info(Thread.currentThread().getName() + " attempting to create new mvconn");
        try {

            Properties props = new Properties();
            props.setProperty("username", pool.getUser());
            props.setProperty("password", pool.getPassword());

            conn = new com.tigr.mvapi.MVConnection(pool.getUrl(), props);
            conn.getConnector().setSoTimeout(timout);
            conn.logTo(pool.getAccount(), pool.getAccountPassword());

            logger.info(Thread.currentThread().getName() + " established connection successfully");

        } catch (MVException e) {

            conn = null;
            String message = Thread.currentThread().getName() + " cannot establish connection or log into provided account: " + StringHelper.getStackTraceAsString(e);
            logger.error(message);
            throw new SQLException(message);

        } catch (SocketException se) {


            killConnection();
            conn = null;
            String message = Thread.currentThread().getName() + " socket error:" + StringHelper.getStackTraceAsString(se);
            logger.error(message);
            throw new SQLException(message);
        } finally {


            this.pool.removeConnection(this);
        }
    }

    public synchronized boolean lease() {
        if (inuse) {
            return false;
        } else {
            inuse = true;
            timestamp = System.currentTimeMillis();
            return true;
        }
    }

    public void killConnection() {

        try {
            if (conn.isClosed() == true) {
                return;
            }
            

        } catch (MVException ex) {

            logger.error(Thread.currentThread().getName() + " Failed checking connection status: " + this.id + " " + StringHelper.getStackTraceAsString(ex));

        }
        
        new Thread(new Runnable() {

            @Override
            public void run() {


                try {

                    logger.info(Thread.currentThread().getName() + " closing out mvbase conn");

                    conn.close();
                    conn.closeConnection();


                    logger.info(Thread.currentThread().getName() + " finished closing out mvbase conn");


                } catch (Exception e) {

                    logger.info(Thread.currentThread().getName() + " exception occured killing the connection.  could just be an exception on socket close: " + StringHelper.getStackTraceAsString(e));
                }


            }
        });



    }

    public boolean validate() {
        try {

            conn.getMetaData();


        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean inUse() {
        return inuse;
    }

    public long getLastUse() {
        return timestamp;
    }

    public void expireLease() {

        inuse = false;
    }

    public boolean pingTest() {

        if (this.inuse == false) {

            try {

                this.inuse = true;
                logger.debug("Pinging connection...");                
                String result = conn.ping();
                logger.debug("Ping test result: " + result);
                this.inuse = false;

            } catch (Exception e) {

                this.inuse = false;
                return false;
            }

            return true;
        }

        return true;
    }

   
    public void close() throws SQLException {


        this.pool.returnConnection(this);
    }

    /**
     * Calls an mv subroutine and returns back the output. 
     * @param params params to pass to the sub should be bar '|' separated
     * @return ResultSet of concrete MVResultSet is returned.  Not all of MVResultSet
     * is implemented yet
     */
    public MVResultSet callMVSub(String subName, String params) throws Exception, SQLException {

        logger.debug(Thread.currentThread().getName() + " made call to callMVSub");

        if (conn.isConnected() == false) {

            logger.error(Thread.currentThread().getName() + " callMVSub was called but no connection was found.  Reconnecting...");
            createNewMVConnection();

        }

        timestamp = System.currentTimeMillis();

        List<String> args = new LinkedList<String>();
        args.add(params);
        args.add("");
        args.add("");
        args.add("");


        try {

            conn.call(subName, args);

            if (!args.get(3).equals("")) {

                String message = Thread.currentThread().getName() + " mv sub call had a handled exception: " + args.get(3);
                logger.debug(message);
                MVResultSet results = new MVResultSet(args.get(3));
                return results;

            }

        } catch (Exception e) {

            String message = Thread.currentThread().getName() + " mv sub call had a unhandled exception. killing connection and removing conn from pool: " + StringHelper.getStackTraceAsString(e);
            logger.error(message);
            killConnection();
            this.pool.removeConnection(this);
            throw new Exception(message);

        }


        MVResultSet results = new MVResultSet(args.get(1));
        return results;

    }

    public MVResultSet callMVSub(String subName, String params, int mvCallTimeOut) throws Exception, SQLException {

        this.mvCallSubTimeOut = mvCallTimeOut;

        return callMVSub(subName, params);

    }

    
   
}
