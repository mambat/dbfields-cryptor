package org.n3r.sensitive.proxy;

import org.n3r.core.security.AesCryptor;
import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class PreparedStmtHandler implements InvocationHandler {
    private final Logger logger = LoggerFactory.getLogger(PreparedStmtHandler.class);

    private final PreparedStatement pstmt;
    private final SensitiveCryptor cryptor;
    private final SensitiveFieldsParser parser;

    public PreparedStmtHandler(PreparedStatement pStmt, SensitiveFieldsParser parser, SensitiveCryptor cryptor) {
        this.pstmt = pStmt;
        this.parser = parser;
        this.cryptor = cryptor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ProxyMethods.requireEncrypt(method.getName())
                && parser.inBindIndice((Integer) args[0])) {
            try {
                args[1] = cryptor.encrypt(args[1].toString());
            } catch (Exception e) {
                logger.warn("Encrypt parameter #{}# error", args[1]);
            }
        }

        Object result = method.invoke(pstmt, args);

        if (ProxyMethods.isGetResult(method.getName())
                && parser.getSecuretResultIndice().size() > 0) {
            result = new ResultSetHandler((ResultSet) result, parser, cryptor).getResultSet();
        }

        return result;
    }

    public PreparedStatement getStatement() {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, this);
    }
}
