package org.n3r.sensitive.proxy;

import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetHandler implements InvocationHandler {
    private Logger logger = LoggerFactory.getLogger(ResultSetHandler.class);
    private ResultSet resultSet;
    private SensitiveFieldsParser parser;
    private SensitiveCryptor cryptor;

    public ResultSetHandler(ResultSet resultSet, SensitiveFieldsParser parser,
                            SensitiveCryptor cryptor) throws SQLException {
        this.resultSet = resultSet;
        this.parser = parser;
        this.cryptor = cryptor;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(resultSet, args);

        if (ProxyMethods.requireDecrypt(method.getName())
                && parser.inResultIndice(args[0]))
            try {
                result = cryptor.decrypt(result.toString());
            } catch(Exception e) {
                logger.warn("Decrypt result #{}# error", result);
            }

        return result;
    }

    public ResultSet getResultSet() {
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class}, this);
    }
}
