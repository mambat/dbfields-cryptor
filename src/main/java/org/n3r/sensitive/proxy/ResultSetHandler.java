package org.n3r.sensitive.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.n3r.core.security.BaseCryptor;
import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultSetHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(PreparedStatementHandler.class);
    private ResultSet resultSet;
    private SensitiveFieldsParser parser;
    private BaseCryptor cryptor;

    public ResultSetHandler(ResultSet resultSet, SensitiveFieldsParser parser, BaseCryptor cryptor) throws SQLException {
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

}
