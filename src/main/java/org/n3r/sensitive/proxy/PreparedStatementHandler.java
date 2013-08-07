package org.n3r.sensitive.proxy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.collections.CollectionUtils;
import org.n3r.core.security.BaseCryptor;
import org.n3r.sensitive.parser.SensitiveFieldsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private SensitiveFieldsParser parser;
    private BaseCryptor cryptor;

    public static CacheLoader<String, SensitiveFieldsParser> loader =
            new CacheLoader<String, SensitiveFieldsParser>() {
                @Override
                public SensitiveFieldsParser load(String key) throws Exception {
                    return SensitiveFieldsParser.parseSecuretFields(key);
                }
            };
    public static LoadingCache<String, SensitiveFieldsParser> cache =
            CacheBuilder.newBuilder()
                    .build(loader);

    public PreparedStatementHandler(Connection connection, String sql, BaseCryptor cryptor) throws SQLException {
        this.pstmt = connection.prepareStatement(sql);
        // TODO：sql parse result cache
        this.parser = cache.getUnchecked(sql);
        this.cryptor = cryptor;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ProxyMethods.requireEncrypt(method.getName())
                && parser.inBindIndice((Integer) args[0]))
            try{
                args[1] = cryptor.encrypt(args[1].toString());
            } catch(Exception e) {
                logger.warn("Encrypt parameter #{}# error", args[1]);
            }

        Object result = method.invoke(pstmt, args);

        if (ProxyMethods.isGetResult(method.getName())
                && parser.getSecuretResultIndice().size() > 0) {
            ResultSetHandler rsHandler = new ResultSetHandler((ResultSet) result, parser, cryptor);
            return Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { ResultSet.class }, rsHandler);
        }

        return result;
    }

    public PreparedStatement getPreparedStatement() {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { PreparedStatement.class }, this);
    }

    public SensitiveFieldsParser getSensitiveFieldsParser() {
        return parser;
    }


}
