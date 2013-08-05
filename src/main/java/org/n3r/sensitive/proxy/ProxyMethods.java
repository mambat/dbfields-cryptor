package org.n3r.sensitive.proxy;

import java.util.Set;

import com.google.common.collect.Sets;

public class ProxyMethods {
    private static final Set<String> REQUIRED_ENCRYPT_METHODS = Sets.newHashSet("setString", "setObject");
    private static final Set<String> REQUIRED_DECRYPT_METHODS = Sets.newHashSet("getString", "getObject");
    private static final Set<String> RETURN_RESULTSET_METHODS = Sets.newHashSet("executeQuery", "getResultSet");

    public static boolean requireEncrypt(String method) {
        return REQUIRED_ENCRYPT_METHODS.contains(method);
    }

    public static boolean requireDecrypt(String method) {
        return REQUIRED_DECRYPT_METHODS.contains(method);
    }

    public static boolean isGetResult(String method) {
        return RETURN_RESULTSET_METHODS.contains(method);
    }

}
