package org.n3r.sensitive;


import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.n3r.core.security.AesCryptor;
import org.n3r.sensitive.proxy.AesSensitiveCrypter;
import org.n3r.sensitive.proxy.ConnectionHandler;
import org.n3r.sensitive.proxy.SensitiveCryptor;

import java.sql.*;
import java.util.Calendar;

public class ConnectionProxyTest {
   /*  @Test
    public void testConnectionHandler(){
       try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Where is your Oracle JDBC Driver?", e);
        }

        try {
            CONNECTION = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Connection Failed! Check output console.", e);
        }

        Connection connection =  new ConnectionHandler(CONNECTION).getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select * from person");
        } catch (SQLException e) {
            e.printStackTrace();
        }



    }*/

    private final static String DRIVER_NAME = "oracle.jdbc.OracleDriver";
    private final static String URL = "jdbc:oracle:thin:@127.0.0.1:1521:orcl";
    private final static String USER = "xule";
    private final static String PASSWORD = "xule";
    private final static String PSPT_TYPE_CODE = "01";
    private final static SensitiveCryptor cryptor = new AesSensitiveCrypter();
    private static Connection CONNECTION;

    @BeforeClass
    public static void beforeClass() throws SQLException {
        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Where is your Oracle JDBC Driver?", e);
        }

        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            CONNECTION = new ConnectionHandler(connection, cryptor).getConnection();
            //CONNECTION =DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Connection Failed! Check output console.", e);
        }

        if (CONNECTION == null)
            throw new RuntimeException(
                    "Failed to make connection! connection is null.");

        deleteTables();

        // create tables
        Statement create = null;
        try {
            create = CONNECTION.createStatement();
            create.execute("CREATE TABLE TF_B_ORDER_NETIN (PSPT_NO VARCHAR2(128), PSPT_TYPE_CODE VARCHAR2(2), UPDATETIME DATE)");
            create.execute("CREATE TABLE TF_B_ORDER (ORDER_ID VARCHAR2(128), PSPT_NO VARCHAR2(128))");
            create.execute("create or replace procedure prc_Test1(encryptPsptNo1 in varchar2,p_out out varchar2) is \n" +
                    "begin\n" +
                    "    p_out := encryptPsptNo1;  \n" +
                    "end prc_Test1;");
        } finally {
            if (create != null)
                create.close();
        }

    }

    @AfterClass
    public static void afterClass() throws SQLException {
        deleteTables();
        CONNECTION.close();
    }

    private static void deleteTables() throws SQLException {
        if (CONNECTION == null)
            return;

        Statement create = null;
        try {
            create = CONNECTION.createStatement();
            create.execute("DROP TABLE TF_B_ORDER_NETIN");
            create.execute("DROP TABLE TF_B_ORDER");
            create.execute("DROP PROCEDURE prc_Test1");
        } catch (Exception ex) {
            // Ignore
        }
        finally {
            if (create != null)
                create.close();

        }
    }

    @Test
    public void testSelect() throws SQLException {
        String psptNo = insertOrderNetIn();

        String sql = "SELECT PSPT_NO FROM TF_B_ORDER_NETIN " +
                "WHERE PSPT_NO = ?";
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            // select (use original psptNo)
            //pstmt = new PreparedStatementHandler(CONNECTION, sql, cryptor).getStatement();
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                // resultSet.getString(1)
                System.out.println(psptNo);
                Assert.assertEquals(psptNo, resultSet.getString("PSPT_NO"));
            } else
                Assert.fail("ResultSet is empty！");
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }

    private String insertOrderNetIn() throws SQLException {
        return insertOrderNetIn(generateId());
    }

    private String generateId() {
        return Calendar.getInstance().getTimeInMillis() + "";
    }
    private String insertOrderNetIn(String psptNo) throws SQLException {
        String sql = "INSERT INTO TF_B_ORDER_NETIN (PSPT_NO, PSPT_TYPE_CODE, UPDATETIME) VALUES (?, ?, sysdate)";
        PreparedStatement pstmt = null;
        try {
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            pstmt.setString(2, PSPT_TYPE_CODE);
            pstmt.execute();
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
        return psptNo;
    }

}