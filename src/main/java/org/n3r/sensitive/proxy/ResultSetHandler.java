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
    private SensitiveFieldsParser visitor;
    private BaseCryptor cryptor;

    public ResultSetHandler(ResultSet resultSet, SensitiveFieldsParser visitor, BaseCryptor cryptor) throws SQLException {
        this.resultSet = resultSet;
        this.visitor = visitor;
        this.cryptor = cryptor;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(resultSet, args);

        if (ProxyMethods.requireDecrypt(method.getName()) &&
                (visitor.getSecuretResultIndice().contains(args[0])
                        || visitor.getSecuretResultLabels().contains(args[0])))
            try {
                result = cryptor.decrypt((String) result);
            } catch(Exception e) {
                logger.warn("Decrypt result #{}# error", result);
            }

        return result;
    }

}
