package org.orphanware.mv.sessionmgr;

import com.tigr.mvapi.exceptions.MVException;
import java.net.SocketException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import org.orphanware.utils.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author arash
 */
public class MVConnection implements Connection {

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

            logger.error(Thread.currentThread().getName() + " Failed when checking mvconnection status: " + StringHelper.getStackTraceAsString(ex));

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

    @Override
    public void close() throws SQLException {


        this.pool.returnConnection(this);
    }

    /**
     * Calls an mv subroutine and returns back the output. 
     * @param params params to pass to the sub should be bar '|' separated
     * @return ResultSet of concrete MVResultSet is returned.  Not all of MVResultSet
     * is implemented yet
     */
    public ResultSet callMVSub(String subName, String params) throws Exception, SQLException {

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
                throw new Exception(message);

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

    public ResultSet callMVSub(String subName, String params, int mvCallTimeOut) throws Exception, SQLException {

        this.mvCallSubTimeOut = mvCallTimeOut;

        return callMVSub(subName, params);

    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return conn.prepareCall(sql);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return conn.createStatement();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return conn.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return conn.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        conn.commit();
    }

    @Override
    public void rollback() throws SQLException {
        conn.rollback();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return conn.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return conn.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        conn.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return conn.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        conn.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return conn.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        conn.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return conn.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return conn.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        conn.clearWarnings();
    }

    @Override
    public Array createArrayOf(String string, Object[] os) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWrapperFor(Class type) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object unwrap(Class type) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Statement createStatement(int i, int i1) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Statement createStatement(int i, int i1, int i2) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getClientInfo(String string) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map getTypeMap() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isValid(int i) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallableStatement prepareCall(String string, int i, int i1) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallableStatement prepareCall(String string, int i, int i1, int i2) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PreparedStatement prepareStatement(String string, int i, int i1) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PreparedStatement prepareStatement(String string, int i, int i1, int i2) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PreparedStatement prepareStatement(String string, int i) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PreparedStatement prepareStatement(String string, int[] ints) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PreparedStatement prepareStatement(String string, String[] strings) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void releaseSavepoint(Savepoint svpnt) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void rollback(Savepoint svpnt) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setClientInfo(String string, String string1) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setClientInfo(Properties prprts) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHoldability(int i) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Savepoint setSavepoint(String string) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTypeMap(Map map) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Struct createStruct(String string, Object[] os) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void abort(Executor exctr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSchema() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNetworkTimeout(Executor exctr, int i) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSchema(String string) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
