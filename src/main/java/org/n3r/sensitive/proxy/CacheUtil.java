package org.n3r.sensitive.proxy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.n3r.sensitive.parser.SensitiveFieldsParser;

public class CacheUtil {
    private static CacheLoader<String, SensitiveFieldsParser> loader =
            new CacheLoader<String, SensitiveFieldsParser>() {
                @Override
                public SensitiveFieldsParser load(String sql) throws Exception {
                    return SensitiveFieldsParser.parseSecuretFields(sql);
                }
            };
    private static LoadingCache<String, SensitiveFieldsParser> cache =
            CacheBuilder.newBuilder()
                    .build(loader);

    public static SensitiveFieldsParser getParser(String sql) {
        return cache.getUnchecked(sql);
    }
}
