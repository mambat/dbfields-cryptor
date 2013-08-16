package org.n3r.sensitive.proxy;

import org.n3r.sensitive.parser.SensitiveFieldsParser;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ConnectionHandler implements InvocationHandler {
    private Connection connection;
    private SensitiveCryptor cryptor;

    public ConnectionHandler(Connection connection, SensitiveCryptor cryptor) {
        this.connection = connection;
        this.cryptor = cryptor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object invoke = method.invoke(connection, args);

        if ("prepareStatement".equals(method.getName())) {
            String sql = (String) args[0];
            SensitiveFieldsParser parser = CacheUtil.getParser(sql);
            if (!parser.haveSecureFields()) return invoke;

            return new PreparedStmtHandler((PreparedStatement) invoke, parser, cryptor).getStatement();
        } else if ("prepareCall".equals(method.getName())) {
            String sql = (String) args[0];
            SensitiveFieldsParser parser = CacheUtil.getParser(sql);
            if (!parser.haveSecureFields()) return invoke;

            return new CallableStmtHandler((CallableStatement) invoke, parser, cryptor).getStatement();
        }

        return invoke;
    }

    public Connection getConnection() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Connection.class}, this);
    }
}
