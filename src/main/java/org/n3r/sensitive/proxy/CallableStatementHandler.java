package org.n3r.sensitive.proxy;

import org.n3r.core.security.BaseCryptor;
import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;


public class CallableStatementHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(CallableStatementHandler.class);
    private CallableStatement cstmt;
    private SensitiveFieldsParser parser;
    private BaseCryptor cryptor;


    public CallableStatementHandler(Connection connection, String sql, BaseCryptor cryptor) throws SQLException {
        this.parser = CacheUtil.getParser(sql);
        this.cstmt = connection.prepareCall(sql);
        this.cryptor = cryptor;
    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ProxyMethods.requireEncrypt(method.getName())
                && parser.inBindIndice((Integer) args[0]))
            try {
                args[1] = cryptor.encrypt(args[1].toString());
            } catch (Exception e) {
                logger.warn("Encrypt parameter #{}# error", args[1]);
            }

        return method.invoke(cstmt, args);

    }

    public CallableStatement getCallableStatement() {
        if (!parser.haveSecureFields()) return cstmt;

        return (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{CallableStatement.class}, this);
    }


}

