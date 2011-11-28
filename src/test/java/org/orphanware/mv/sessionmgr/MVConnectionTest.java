package org.orphanware.mv.sessionmgr;

import org.orphanware.mv.sessionmgr.MVConnection;
import org.orphanware.mv.sessionmgr.MVResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import junit.framework.TestCase;

/**
 *
 * @author arash
 */
public class MVConnectionTest extends TestCase {

    class TestMVThread extends Thread {

        private Boolean threadSuccess;
        private String progName, progParams;

        public Boolean getThreadSuccess() {
            return threadSuccess;
        }

        public TestMVThread(String str, String progName, String progParams) {
            super(str);
            
            this.progName = progName;
            this.progParams = progParams;
        }

        @Override
        public void run() {

            threadSuccess = false;
          
            try {


                MVSessionMgr mvSessionMgr = MVSessionMgr.getInstance();
                mvSessionMgr.setUser("MVSP");
                mvSessionMgr.setPassword("");
                mvSessionMgr.setAccount("JAVA.TEST");
                mvSessionMgr.setAccountPassword("");
                mvSessionMgr.setUrl("jdbc:mv:mvbase:10.1.1.8:9010");
                MVConnection conn = (MVConnection) mvSessionMgr.getConnection();
                
            
                MVResultSet results = null;
                results = (MVResultSet) conn.callMVSub(progName, progParams);
           
                System.out.println(getName() + ": " + results.getResultStr() + " DONE!");

                conn.close();
            } catch(Exception e) {
                
                 e.printStackTrace();
                
                 
            }

        }
    }

    public MVConnectionTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    /*
    public void testConnectionMultiThreadDoubleStale() throws SQLException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        
        
        Connection conn = null;
        int totalThreads = 2;
        
        TestMVThread[] threads = new TestMVThread[totalThreads];
        for (int i = 0; i < totalThreads; i++) {
            threads[i] = new TestMVThread("Thread" + i, conn, "GET.INVENTORY", "|1");
            
            threads[i].start();

        }




        boolean atLeastOneThreadAlive;
        while (true) {

            Thread.currentThread().sleep(500);
            atLeastOneThreadAlive = false;

            for (TestMVThread thread : threads) {

                if (thread.isAlive()) {

                    atLeastOneThreadAlive = true;
                    break;

                }
            }

            if(!atLeastOneThreadAlive)
                break;


        }     

        assertTrue(true);

    }
    */
    
    
    
    public void testConnectionMultiThreadStaleContinue() throws SQLException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        int totalThreads = 10;

        TestMVThread[] threads = new TestMVThread[totalThreads];
        for (int i = 0; i < totalThreads; i++) {
            if(i == 0) {
                
                threads[i] = new TestMVThread("Thread" + i, "HELLO.JAVA", "|1");
                threads[i].start();
                
            } else       {        
                
             
                threads[i] = new TestMVThread("Thread" + i, "HELLO.JAVA", "");
                threads[i].start();
            }
            
             
 
            
            

        }

        boolean atLeastOneThreadAlive;
        while (true) {

            Thread.currentThread().sleep(500);
            atLeastOneThreadAlive = false;

            for (TestMVThread thread : threads) {

                if (thread.isAlive()) {

                    atLeastOneThreadAlive = true;
                    break;

                }
            }

            if(!atLeastOneThreadAlive)
                break;


        }     

        assertTrue(true);

    }
    
     
    /*
    
    public void testConnectionMultiThreadNoSubRecovery() throws SQLException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {

   
        Connection conn = null;
        int totalThreads = 2;

        TestMVThread[] threads = new TestMVThread[totalThreads];
        for (int i = 0; i < totalThreads; i++) {
           
            if(i == 0)
                threads[i] = new TestMVThread("Thread" + i, conn, "I.DON'T.EXIST", ""); 
            else
               threads[i] = new TestMVThread("Thread" + i, conn, "HELLO.JAVA", ""); 
               
            
            
            threads[i].start();

        }

        boolean atLeastOneThreadAlive;
        while (true) {

            Thread.currentThread().sleep(500);
            atLeastOneThreadAlive = false;

            for (TestMVThread thread : threads) {

                if (thread.isAlive()) {

                    atLeastOneThreadAlive = true;
                    break;

                }
            }

            if(!atLeastOneThreadAlive)
                break;


        }     

        assertTrue(true);

    }
    
    */
     
    
    /*
    public void testConnectionMultiThreadLoad() throws SQLException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {

  
        Connection conn = null;
        int totalThreads = 20;

        TestMVThread[] threads = new TestMVThread[totalThreads];
        for (int i = 0; i < totalThreads; i++) {
           
               threads[i] = new TestMVThread("Thread" + i, conn, "HELLO.JAVA", ""); 
            
            
            threads[i].start();

        }

        boolean atLeastOneThreadAlive;
        while (true) {

            Thread.currentThread().sleep(500);
            atLeastOneThreadAlive = false;

            for (TestMVThread thread : threads) {

                if (thread.isAlive()) {

                    atLeastOneThreadAlive = true;
                    break;

                }
            }

            if(!atLeastOneThreadAlive)
                break;


        }     

        assertTrue(true);

    }
    */
    /*
    public void testConnectionMultiThreadWrongParamsRecover() throws SQLException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        
   
        Connection conn = null;
        int totalThreads = 2;

        TestMVThread[] threads = new TestMVThread[totalThreads];
        for (int i = 0; i < totalThreads; i++) {
           
            if(i == 0)
                threads[i] = new TestMVThread("Thread" + i, conn, "GET.MAT", ""); 
            else
               threads[i] = new TestMVThread("Thread" + i, conn, "HELLO.JAVA", ""); 
               
            
            
            threads[i].start();

        }

        boolean atLeastOneThreadAlive;
        while (true) {

            Thread.currentThread().sleep(500);
            atLeastOneThreadAlive = false;

            for (TestMVThread thread : threads) {

                if (thread.isAlive()) {

                    atLeastOneThreadAlive = true;
                    break;

                }
            }

            if(!atLeastOneThreadAlive)
                break;


        }     

        assertTrue(true);

    }
     
     */
    
}
