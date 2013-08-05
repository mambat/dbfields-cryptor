package org.n3r.sensitive.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.n3r.core.security.BaseCryptor;
import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreparedStatementHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(PreparedStatementHandler.class);
    private PreparedStatement pstmt;
    private SensitiveFieldsParser visitor;
    private BaseCryptor cryptor;

    public PreparedStatementHandler(Connection connection, String sql, BaseCryptor cryptor) throws SQLException {
        this.pstmt = connection.prepareStatement(sql);
        // TODO：sql parse result cache
        this.visitor = SensitiveFieldsParser.parseSecuretFields(sql);
        this.cryptor = cryptor;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ProxyMethods.requireEncrypt(method.getName())
                && visitor.inBindIndice((Integer) args[0]))
            try{
                args[1] = cryptor.encrypt(args[1].toString());
            } catch(Exception e) {
                logger.warn("Encrypt parameter #{}# error", args[1]);
            }

        Object result = method.invoke(pstmt, args);

        if (ProxyMethods.isGetResult(method.getName())
                && visitor.getSecuretResultIndice().size() > 0) {
            ResultSetHandler rsHandler = new ResultSetHandler((ResultSet) result, visitor, cryptor);
            return Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { ResultSet.class }, rsHandler);
        }

        return result;
    }

    public PreparedStatement getPreparedStatement() {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { PreparedStatement.class }, this);
    }

}
