package org.n3r.sensitive.proxy;

import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;

class CallableStmtHandler implements InvocationHandler {
    private final Logger logger = LoggerFactory.getLogger(CallableStmtHandler.class);
    private final CallableStatement stmt;
    private final SensitiveFieldsParser parser;
    private final SensitiveCryptor cryptor;

    public CallableStmtHandler(CallableStatement stmt, SensitiveFieldsParser parser,
                               SensitiveCryptor cryptor) {
        this.stmt = stmt;
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

        return method.invoke(stmt, args);
    }

    public CallableStatement getStatement() {
        return (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{CallableStatement.class}, this);
    }
}
