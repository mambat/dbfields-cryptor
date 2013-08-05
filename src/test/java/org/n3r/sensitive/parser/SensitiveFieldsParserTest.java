package org.n3r.sensitive.parser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.n3r.sensitive.parser.SensitiveFieldsParser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SensitiveFieldsParserTest {
    @Test
    public void testQuery1() {
        String sql = "select d, f, a, b, c from table1 where c = ? and a = ? and b = ?";

        final Set<String> securetFieldsConfig = Sets.newHashSet("table1.a", "table1.b");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(3, 4));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(2, 3));
        List<String> securetResultLabels = visitorAdapter.getSecuretResultLabels();
        assertEquals(securetResultLabels, Lists.newArrayList("a", "b"));

        sql = "select d, f, a as aalias, b, c from table1 where c = ? || ',' || ? and a = ? and b = ?";

        visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(3, 4));
        securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(3, 4));
        securetResultLabels = visitorAdapter.getSecuretResultLabels();
        assertEquals(securetResultLabels, Lists.newArrayList("aalias", "b"));
    }


    @Test
    public void testQuery2() {
        String sql = "select t.a, t.b as tb, t.c from table1 t where t.a = ?";

        final Set<String> securetFieldsConfig = Sets.newHashSet("table1.a", "table1.b");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(1, 2));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(1));
        //List<String> securetResultLabels = visitorAdapter.getSecuretResultLabels();
        // TODO: t.a or a
        //assertEquals(securetResultLabels, Lists.newArrayList("t.a", "b"));
    }

    @Test
    public void testQuery3() {
        String sql = "select t1.a, t1.b, t2.c from table1 t1, table2 t2 where t1.id = ? and t1.id = t2.id and t2.d = ?";
        final Set<String> fields = Sets.newHashSet("table1.a", "table1.b", "table2.d");

        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, fields);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(1, 2));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(2));

    }

    @Test
    public void testQuery4() {
        String sql = "SELECT   T.PSPT_NO \"CertNum\",\n" +
                "         T.PSPT_TYPE_CODE \"CertType\",\n" +
                "         T.PSPT_ADDR \"CertAdress\", \n" +
                "         T.CUST_NAME \"CustomerName\",\n" +
                "         TO_CHAR(T.PSPT_EXPIRE_DATE,'yyyyMMdd') \"CertExpireDate\",\n" +
                "         T.USER_TAG \"UserType\"\n" +
                "  FROM   TF_B_ORDER_NETIN T\n" +
                " WHERE   T.ORDER_ID = ?";

        final Set<String> securetFieldsConfig = Sets.newHashSet("TF_B_ORDER_NETIN.PSPT_NO", "TF_B_ORDER_NETIN.CUST_NAME");

        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(1, 4));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList());
    }

    @Test
    public void testQuery5() {
        String sql = "SELECT O.ORDER_ID,\n" +
                "               O.PROVINCE_CODE,\n" +
                "               O.CITY_CODE,\n" +
                "               O.DISTRICT_ID,\n" +
                "               O.PAY_TYPE,\n" +
                "               O.INCOME_MONEY,\n" +
                "               O.CUST_ID,\n" +
                "               O.MERCHANT_ID,\n" +
                "               O.POST_ADDR_ID,\n" +
                "               M.MERCHANT_ID,\n" +
                "               M.CHANNEL_ID,\n" +
                "               M.CHANNEL_TYPE,\n" +
                "               S.STAFF_ID,\n" +
                "               S.ESS_STAFF_ID,\n" +
                "               A.ATTR_NAME,\n" +
                "               A.ATTR_CODE,\n" +
                "               A.ATTR_VAL_CODE,\n" +
                "               A.ATTR_VAL_NAME,\n" +
                "               A.EOP_ATTR_CODE,\n" +
                "               A.EOP_ATTR_VAL_CODE,\n" +
                "               A.INST_EACH_ID INST_EACH_ID,\n" +
                "               G.AMOUNT_RECEVABLE,\n" +
                "               G.AMOUNT_RECEIVED,\n" +
                "               G.DERATE_REASON,\n" +
                "               G.AMOUNT_DERATE,\n" +
                "               N.ORDER_ID,\n" +
                "               N.CUST_NAME,\n" +
                "               N.PSPT_TYPE_CODE,\n" +
                "               N.PSPT_NO,\n" +
                "               N.CUST_TAG,\n" +
                "               N.AUTH_TAG,\n" +
                "               OP.RECEIVER_NAME,\n" +
                "               OP.MOBILE_PHONE RECEIVER_PHONE,\n" +
                "               OP.FIX_PHONE\n" +
                "          FROM TF_B_ORDER               O,\n" +
                "               TF_F_MECHANT             M,\n" +
                "               TF_M_STAFF               S,\n" +
                "               TF_B_ORDER_GOODS_ATTRVAL A,\n" +
                "               TF_B_ORDER_GOODS         G,\n" +
                "               TF_B_ORDER_NETIN         N,\n" +
                "               TF_B_ORDER_POST  OP\n" +
                "         WHERE M.MERCHANT_ID = O.MERCHANT_ID \n" +
                "           AND O.ORDER_ID = OP.ORDER_ID(+)\n" +
                "           AND O.PARTITION_ID = OP.PARTITION_ID(+)\n" +
                "           AND A.ORDER_ID = O.ORDER_ID\n" +
                "           AND A.GOODS_ID = G.GOODS_ID\n" +
                "           AND G.ORDER_ID = O.ORDER_ID\n" +
                "           AND N.ORDER_ID = O.ORDER_ID\n" +
                "           AND A.GOODS_INST_ID = ?\n" +
                "           AND G.GOODS_ID = ?\n" +
                "           AND O.ORDER_ID = ?\n" +
                "           AND S.STAFF_ID = ? ";

        final Set<String> securetFieldsConfig = Sets.newHashSet("TF_B_ORDER_NETIN.PSPT_NO", "TF_B_ORDER_NETIN.CUST_NAME");

        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(27, 29));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList());
    }


    @Test
    public void testQuery6() throws Exception {
        String sql = "SELECT TO_CHAR(O.ORDER_ID) ORDER_ID,\n" +
                "       O.ORDER_NO,\n" +
                "       TO_CHAR(O.ORDER_TIME, 'YYYY/MM/DD HH24:MI') AS ORDER_TIME,\n" +
                "       TRUNC(TO_NUMBER(SYSDATE - O.ORDER_TIME)) || '天' ||\n" +
                "       TRUNC(MOD(TO_NUMBER(SYSDATE - O.ORDER_TIME) * 24, 24)) || '时' ||\n" +
                "       TRUNC(MOD(TO_NUMBER(SYSDATE - O.ORDER_TIME) * 24 * 60, 60)) || '分' DELAY_TIME,\n" +
                "       O.ORDER_TOTAL_MONEY/1000 ORDER_TOTAL_MONEY,\n" +
                "       O.PAY_TYPE,\n" +
                "       O.CUST_ID,\n" +
                "       O.LOGIN_NAME,\n" +
                "       O.CUST_IP,\n" +
                "       O.CUST_IP_STR,\n" +
                "       DECODE(O.DELIVER_TYPE_CODE, '01', '快递', '03', '来联通自提') DELIVER_TYPE_NAME,\n" +
                "       SELFFETCH.SELFGET_ADDR_NAME,\n" +
                "       OG.GOODS_CTLG_CODE,\n" +
                "       T.PAY_TYPE_NAME,\n" +
                "       TC.TMPL_CTGR_RNAME,\n" +
                "       P.POST_ADDR,\n" +
                "       P.PROVINCE_CODE,\n" +
                "       P.CITY_CODE,\n" +
                "       P.DISTRICT_CODE,\n" +
                "       DECODE(OG.TMPL_ID, '10000011', '', '10000012', '', NE.PRE_NUM) ATTR_VAL_CODE,\n" +
                "       CITY.CITY_NAME AS NUM_AREA_NAME,\n" +
                "       O.PROVINCE_CODE ESS_PROVINCE_CODE,\n" +
                "       O.CITY_CODE AS NUM_AREA_CODE,\n" +
                "       DECODE(OG.TMPL_ID, '10000011', ORDERCITY.CITY_NAME, '10000012', ORDERCITY.CITY_NAME, '') WIRECARD_CITYNAME,\n" +
                "       OG.TMPL_ID,\n" +
                "       NVL(CUSTID.PARAM1_VALUE,1) as CUST_ORDER_NUM,\n" +
                "       NVL(CUSTIP.PARAM1_VALUE,1) as IP_ORDER_NUM,\n" +
                "       DECODE(BC.CUST_ID,o.cust_id,'黑名单客户','') AS BLKCUST_ORDER_DESC,\n" +
                "       CASE WHEN O.CONN_CHANNEL = '10' THEN DECODE(O.ORDER_FROM, 'TAOBAO', CA.PARA_CODE2 || '：', 'PAIPAI', CA.PARA_CODE2 || '：', '订单来源：')\n" +
                "            WHEN O.CONN_CHANNEL = '11' THEN '推广联盟：'\n" +
                "            END ORDER_FROM_PRE,\n" +
                "       CASE WHEN O.CONN_CHANNEL = '10' THEN DECODE(O.ORDER_FROM, 'TAOBAO', O.TID, 'PAIPAI', O.TID, CA.PARA_CODE2)\n" +
                "            WHEN O.CONN_CHANNEL = '11' THEN DECODE(LEAGUE.LEAGUE_NAME_ROOT, NULL , LEAGUE.LEAGUE_NAME, LEAGUE.LEAGUE_NAME_ROOT)\n" +
                "            END ORDER_FROM,\n" +
                "       NE.USER_TAG,\n" +
                "       OG.INVENTORY_TYPE\n" +
                "FROM TF_B_ORDER O \n" +
                "LEFT JOIN (SELECT W.ORDER_ID,\n" +
                "                  L.LEAGUE_NAME,\n" +
                "                  T.LEAGUE_NAME LEAGUE_NAME_ROOT\n" +
                "             FROM TF_B_ORDER_WM W, TF_F_LEAGUE L LEFT JOIN TF_F_LEAGUE T ON L.DEPEND_LEAGUE = T.LEAGUE_ID\n" +
                "            WHERE W.WM_P_ID = L.LEAGUE_ID) LEAGUE \n" +
                "                 ON O.ORDER_ID = LEAGUE.ORDER_ID\n" +
                "LEFT JOIN TF_B_ORDER_GOODSINS_ATVAL B ON (O.ORDER_ID = B.ORDER_ID AND O.PARTITION_ID = B.PARTITION_ID AND B.ATTR_CODE = 'A000025')\n" +
                "LEFT JOIN TF_M_CITY CITY ON CITY.CITY_CODE = B.ATTR_VAL_CODE\n" +
                "LEFT JOIN TF_M_CITY ORDERCITY ON ORDERCITY.CITY_CODE = O.CITY_CODE\n" +
                "LEFT JOIN TF_F_CUST_BLKLST BC ON (BC.CUST_ID = O.CUST_ID OR BC.LOGIN_IP = O.CUST_IP)\n" +
                "LEFT JOIN TF_B_ORDER_NETIN NE ON(O.ORDER_ID = NE.ORDER_ID AND O.PARTITION_ID = NE.PARTITION_ID)\n" +
                "LEFT JOIN TS_B_ORDERSTATS_CUSTID CUSTID ON O.CUST_ID = CUSTID.CUST_ID AND CUSTID.PARTITION_ID = MOD(O.CUST_ID, 100)\n" +
                "LEFT JOIN TS_B_ORDERSTATS_CUSTIP CUSTIP ON O.CUST_IP_STR = CUSTIP.CUST_IP_STR AND CUSTIP.PARTITION_ID = MOD(O.CUST_IP_STR, 100)\n" +
                "LEFT JOIN TD_B_COMMPARA CA ON(CA.PARAM_ATTR = '1009'  AND  CA.PARAM_CODE = 'MALL_ORDER_FROM' AND O.ORDER_FROM = CA.PARA_CODE1)\n" +
                "LEFT JOIN TF_B_ORDER_SELFFETCH_RELE SELFFETCH ON(O.ORDER_ID = SELFFETCH.ORDER_ID AND SELFFETCH.PARTITION_ID = O.PARTITION_ID AND SELFFETCH.STATE = '1') \n" +
                "     INNER JOIN TF_M_STAFF_BUSIAREA_RES AREAP ON(AREAP.STAFF_ID = ? AND AREAP.BUSIAREA_TYPE = '1'AND O.PROVINCE_CODE = AREAP.BUSIAREA_CODE),\n" +
                "         TD_B_PAYTYPE T,\n" +
                "         TF_B_ORDER_POST P,\n" +
                "         TF_B_ORDER_GOODSINS OG,\n" +
                "         TD_G_TMPL_CTGR TC \n" +
                " WHERE O.PAY_TYPE = T.PAY_TYPE_CODE\n" +
                "  AND O.ORDER_ID = P.ORDER_ID \n" +
                "  AND O.ORDER_ID = OG.ORDER_ID\n" +
                "  AND O.PARTITION_ID = P.PARTITION_ID \n" +
                "  AND O.PARTITION_ID = OG.PARTITION_ID\n" +
                "  AND OG.GOODS_CTLG_CODE = TC.TMPL_CTGR_CODE \n" +
                "  AND O.ORDER_STATE = ? \n" +
                "  AND O.MERCHANT_ID = ? \n" +
                "  AND O.CLAIM_FLAG = '0'\n" +
                "  AND P.PROVINCE_CODE = ?\n" +
                "  AND P.CITY_CODE = ?\n" +
                "  AND P.DISTRICT_CODE = ?\n" +
                "  AND OG.GOODS_CTLG_CODE = ?\n" +
                "  AND O.PAY_TYPE = ?\n" +
                "  AND O.ORDER_NO = ?\n" +
                "  AND NE.PSPT_NO= ?\n" +
                "ORDER BY O.ORDER_ID";
        final Set<String> securetFieldsConfig = Sets.newHashSet("TF_B_ORDER_NETIN.PSPT_NO", "TF_B_ORDER_NETIN.CUST_NAME");

        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(9));
    }

    @Test
    public void testQuery7() throws Exception {
        String sql = "SELECT TO_CHAR(A.ORDER_ID) ORDER_ID,              A.ORDER_NO,              TO_CHAR(A.ORDER_TIME, 'YYYY-MM-DD HH24:MI') ORDER_TIME,\n" +
                "               A.CUST_REMARK,                             A.INVOCE_TITLE,          A.PAY_STATE,    \n" +
                "               A.PAY_TYPE PAY_TYPE_CODE,                  BD.CONTACT_NAME,         BD.CONTACT_PHONE,\n" +
                "               BD.MERGE_ACCOUNT_PHONE,                    BD.ACCEPT_TYPE,           \n" +
                "               C.PARA_CODE2 PSPT_NAME,                    BD.PSPT_ADDR,            BD.PSPT_NO,\n" +
                "               E.GOODS_NAME,                              V.INVOCE_CONTENT,        SPEED.ATTR_VAL_NAME BROADBAND_SPEED,\n" +
                "               PRODU.ATTR_VAL_NAME PRODUCT_NAME,          BD.DETAIL_INS_ADDR,      BD.OFFICE_DIRECTION_NAME,\n" +
                "               BD.CUST_NAME,    PW.PAY_WAY_NAME PAY_WAY_NAME,             BD.BROADBAND_INS_ADDRCODE,         \n" +
                "               DECODE(BD.INSTALL_TYPE,'01','',BD.OFFICE_DIRECTION_NAME) OFFICE_DIRECTION_NAME,\n" +
                "               DECODE(BD.INSTALL_TYPE,'01',S.OFFICE_DIRECTION_CODE,'02',BD.OFFICE_DIRECTION_CODE) OFFICE_DIRECTION_CODE,\n" +
                "               BD.PSPT_TYPE_CODE,                         E.ACT_TYPE,              E.SALEACT_ID,\n" +
                "               E.VER_NO,                                  A.PROVINCE_CODE,         A.CITY_CODE,\n" +
                "               A.ORDER_STATE,                             BD.PROCESS_STATE,        E.GOODS_ID,\n" +
                "               PRODU.ATTR_VAL_CODE PRODUCT_CODE,          BD.INSTALL_TYPE,         BD.ACCESS_MODE,\n" +
                "               S.SHARE_NUMBER,                            S.AREA_CODE,\n" +
                "               DECODE(A.PAY_TYPE, '01','在线支付','02','上门收费')         PAY_TYPE_DESC,\n" +
                "               TO_CHAR(A.ORIGINAL_PRICE / 1000, 'FM9999999990.00')    ORIGINAL_PRICE,\n" +
                "               TO_CHAR(A.INCOME_MONEY / 1000, 'FM9999999990.00')      INCOME_MONEY\n" +
                "          FROM TF_B_ORDER                      A,\n" +
                "               TD_B_PAYWAY                     PW,\n" +
                "               TD_B_INVOICE                    V,\n" +
                "               TF_B_ORDER_GOODSINS             E,\n" +
                "               TF_B_ORDER_BROADBAND            BD,\n" +
                "               TF_B_ORDER_SHARENUM             S,\n" +
                "               (SELECT ORDER_ID, PARTITION_ID, ATTR_VAL_CODE, ATTR_VAL_NAME \n" +
                "                  FROM TF_B_ORDER_GOODSINS_ATVAL \n" +
                "                 WHERE ATTR_CODE='A000060')    SPEED,\n" +
                "                 \n" +
                "               (SELECT ORDER_ID, PARTITION_ID, ATTR_VAL_CODE, ATTR_VAL_NAME\n" +
                "                  FROM TF_B_ORDER_GOODSINS_ATVAL\n" +
                "                 WHERE ATTR_CODE='A000061')    PRODU,\n" +
                "                 \n" +
                "               (SELECT PARA_CODE1, PARA_CODE2 \n" +
                "                  FROM TD_B_COMMPARA \n" +
                "                 WHERE PARAM_ATTR = '1002')    C\n" +
                "         WHERE A.ORDER_ID = E.ORDER_ID\n" +
                "           AND A.PARTITION_ID = E.PARTITION_ID \n" +
                "           AND A.ORDER_ID = BD.ORDER_ID\n" +
                "           AND A.PARTITION_ID = BD.PARTITION_ID\n" +
                "           AND A.ORDER_ID = ?\n" +
                "           AND A.PARTITION_ID = MOD(?,100)\n" +
                "           \n" +
                "           AND A.PAY_WAY = PW.PAY_WAY_CODE(+)\n" +
                "           AND A.PAY_TYPE = PW.PAY_TYPE_CODE(+)\n" +
                "           AND A.INVO_CONT_CODE = V.INVO_CONT_CODE(+)\n" +
                "           AND A.ORDER_ID = SPEED.ORDER_ID(+)\n" +
                "           AND A.PARTITION_ID = SPEED.PARTITION_ID(+)\n" +
                "           AND A.ORDER_ID = PRODU.ORDER_ID(+)\n" +
                "           AND A.PARTITION_ID = PRODU.PARTITION_ID(+)\n" +
                "           AND BD.ORDER_ID = S.ORDER_ID(+)\n" +
                "           AND BD.PSPT_TYPE_CODE = C.PARA_CODE1(+)";
        final Set<String> securetFieldsConfig = Sets.newHashSet("TF_B_ORDER_BROADBAND.PSPT_NO");

        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList(14));
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList());
    }

    @Test
    public void testInsert1() {
        String sql = "insert into table1(a, b, c) values(?, ?, ?)";
        final Set<String> securetFieldsConfig = Sets.newHashSet("table1.a", "table1.b");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(1, 2));

        sql = "insert into table1(a, b, c) values(? || 'x' || ?, ?, ?)";
        visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(3));
    }

    @Test
    public void testUpdate() {
        String sql = "update table1 t1 set t1.a = ?, t1.b = ?, t1.c = ?";
        final Set<String> securetFieldsConfig = Sets.newHashSet("table1.a", "table1.b");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(1, 2));

        sql = "update table1  set a = ? || 'X' || ?, b = ?, c = ?";
        visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(3));
    }

    @Test
    public void testMerge() {
        String sql = "MERGE INTO copy_emp c " +
                "USING employees e " +
                "ON (c.employee_id=e.employee_id) " +
                "WHEN MATCHED THEN " +
                "UPDATE SET " +
                "c.first_name=?, " +
                "c.last_name=e.last_name, " +
                "c.department_id=? " +
                "WHEN NOT MATCHED THEN " +
                "INSERT(employee_id,first_name,last_name," +
                "email,phone_number,hire_date,job_id," +
                "salary,commission_pct,manager_id,department_id) " +
                "VALUES(?, ?, ?," +
                "e.email,e.phone_number,e.hire_date,e.job_id, " +
                "e.salary,e.commission_pct,e.manager_id,?)";

        final Set<String> securetFieldsConfig = Sets.newHashSet("copy_emp.first_name", "copy_emp.department_id");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(1, 2, 4, 6));
    }

    @Test
    public void testProcedure() {
        String sql = "{call abc.myproc(?,?,?)}";
        final Set<String> securetFieldsConfig = Sets.newHashSet("abc.myproc.2");
        SensitiveFieldsParser visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        List<Integer> securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        List<Integer> securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(2));

        sql = "{call abc.myproc(? || 'x' || ?,?,?)}";
        visitorAdapter = SensitiveFieldsParser.parseSecuretFields(sql, securetFieldsConfig);

        securetResultIndice = visitorAdapter.getSecuretResultIndice();
        assertEquals(securetResultIndice, Lists.newArrayList());
        securetBindIndice = visitorAdapter.getSecuretBindIndice();
        assertEquals(securetBindIndice, Lists.newArrayList(3));
    }


}
