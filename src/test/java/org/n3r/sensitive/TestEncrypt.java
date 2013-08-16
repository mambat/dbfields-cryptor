package org.n3r.sensitive;


import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n3r.sensitive.proxy.AesSensitiveCrypter;
import org.n3r.sensitive.proxy.ConnectionHandler;
import org.n3r.sensitive.proxy.SensitiveCryptor;

import java.sql.*;
import java.util.Calendar;

public class TestEncrypt {

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
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Connection Failed! Check output console.", e);
        }

        if (CONNECTION == null)
            throw new RuntimeException(
                    "Failed to make connection! connection is null.");

        deleteTables();

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
        } finally {
            if (create != null)
                create.close();

        }
    }

    @Test
    public void testInsert() throws SQLException {
        String psptNo = insertOrderNetIn();

        String sql = "SELECT PSPT_NO FROM TF_B_ORDER_NETIN " +
                "WHERE PSPT_NO = ?";
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();
            if (resultSet.next())
                Assert.assertEquals(psptNo, resultSet.getString(1));
            else
                Assert.fail("ResultSet is empty！");
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
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
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();
            if (resultSet.next())
                Assert.assertEquals(psptNo, resultSet.getString("PSPT_NO"));
            else
                Assert.fail("ResultSet is empty！");
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }

    @Test
    public void testUpdate() throws SQLException {
        String psptNo = insertOrderNetIn();

        String sql = "UPDATE TF_B_ORDER_NETIN N " +
                "SET N.PSPT_TYPE_CODE = ? " +
                "WHERE N.PSPT_NO = ?";
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        String psptTypeCode = "00";

        try {
            // update
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptTypeCode);
            pstmt.setString(2, psptNo);
            pstmt.execute();

            sql = "SELECT PSPT_TYPE_CODE FROM TF_B_ORDER_NETIN WHERE PSPT_NO = ?";
           // select and compare
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();

            if (resultSet.next())
                Assert.assertEquals(psptTypeCode, resultSet.getString(1));
            else
                Assert.fail("ResultSet is empty！");
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }

    @Test
    public void testMerge() throws SQLException {
        String sql = "MERGE INTO TF_B_ORDER_NETIN N " +
                "USING TF_B_ORDER R " +
                "ON (R.PSPT_NO = N.PSPT_NO) " +
                "WHEN MATCHED THEN " +
                "UPDATE SET " +
                "N.PSPT_TYPE_CODE = ? " +
                "WHEN NOT MATCHED THEN " +
                "INSERT(PSPT_NO, PSPT_TYPE_CODE, UPDATETIME) " +
                "VALUES(?, ?, sysdate)";

        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        String psptTypeCodeUpdated = "ox";
        try {
            // not matched
            String psptNo = insertOrder(generateId());

            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptTypeCodeUpdated);
            pstmt.setString(2, psptNo);
            pstmt.setString(3, PSPT_TYPE_CODE);
            pstmt.execute();

            pstmt = CONNECTION.prepareStatement("SELECT PSPT_NO FROM TF_B_ORDER_NETIN WHERE PSPT_NO = ? AND PSPT_TYPE_CODE = ?");
            pstmt.setString(1, psptNo);
            pstmt.setString(2, PSPT_TYPE_CODE);
            resultSet = pstmt.executeQuery();
            if (resultSet.next())
                Assert.assertEquals(psptNo, resultSet.getString(1));
            else
                Assert.fail("ResultSet is empty！");

            // matched
            pstmt = CONNECTION.prepareStatement(sql);;
            pstmt.setString(1, psptTypeCodeUpdated);
            pstmt.setString(2, psptNo);
            pstmt.setString(3, PSPT_TYPE_CODE);
            pstmt.execute();

            pstmt = CONNECTION.prepareStatement("SELECT PSPT_NO FROM TF_B_ORDER_NETIN WHERE PSPT_NO = ? AND PSPT_TYPE_CODE = ?");
            pstmt.setString(1, psptNo);
            pstmt.setString(2, psptTypeCodeUpdated);
            resultSet = pstmt.executeQuery();
            if (resultSet.next())
                Assert.assertEquals(psptNo, resultSet.getString(1));
            else
                Assert.fail("ResultSet is empty！");

        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }

    @Test
    public void testDelete() throws SQLException {
        String psptNo = insertOrderNetIn();
        String sql = "DELETE FROM TF_B_ORDER_NETIN " +
                "WHERE PSPT_NO = ?";
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            pstmt.execute();

            // select and compare
            pstmt =CONNECTION.prepareStatement("SELECT PSPT_NO FROM TF_B_ORDER_NETIN WHERE PSPT_NO = ?");
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();

            if (resultSet.next())
                Assert.fail("Delete is not work");
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }

    @Test
    public void testProcedure() throws SQLException {
        String psptNo = insertOrderNetIn();
        String sql = "{ call prc_Test1(?, ?) }";
        //"SELECT PSPT_NO FROM TF_B_ORDER_NETIN " +
        //"WHERE PSPT_NO = ?";
        CallableStatement cstmt = null;
        ResultSet resultSet = null;

        try {
            // select (use encrypted psptNo) and compare
            String encryptPsptNo = cryptor.encrypt(psptNo);
            CONNECTION.setAutoCommit(false);
            cstmt = CONNECTION.prepareCall(sql);
            cstmt.setString(1, psptNo);
            cstmt.registerOutParameter(2, Types.VARCHAR);
            cstmt.execute();
            CONNECTION.commit();
            String result = cstmt.getString(2);
            Assert.assertEquals(encryptPsptNo, result);
        } finally {
            if (cstmt != null)
                cstmt.close();
            if (resultSet != null)
                resultSet.close();
        }
    }



    @Test
    public void testConnectionHandler() throws SQLException {
        String psptNo = insertOrderNetIn();

        String sql = "SELECT PSPT_NO FROM TF_B_ORDER_NETIN " +
                "WHERE PSPT_NO = ?";
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            //preparedStatement
            pstmt = CONNECTION.prepareStatement(sql);
            pstmt.setString(1, psptNo);
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                Assert.assertEquals(psptNo, resultSet.getString("PSPT_NO"));
            } else
                Assert.fail("ResultSet is empty！");


            //callableStatement
            String encryptPsptNo = cryptor.encrypt(psptNo);
            CONNECTION.setAutoCommit(false);
            sql = "{ call prc_Test1(?, ?) }";
            CallableStatement cstmt = CONNECTION.prepareCall(sql);
            cstmt.setString(1, psptNo);
            cstmt.registerOutParameter(2, Types.VARCHAR);
            cstmt.execute();
            CONNECTION.commit();
            String result = cstmt.getString(2);
            Assert.assertEquals(encryptPsptNo, result);
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

    private String insertOrder(String psptNo) throws SQLException {
        String sql = "INSERT INTO TF_B_ORDER (ORDER_ID, PSPT_NO) VALUES (?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt =  CONNECTION.prepareStatement(sql);
            pstmt.setString(1, generateId());
            pstmt.setString(2, psptNo);
            pstmt.execute();
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
        return psptNo;
    }

}