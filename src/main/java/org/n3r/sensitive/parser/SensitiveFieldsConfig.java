package org.n3r.sensitive.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class SensitiveFieldsConfig {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFieldsConfig.class);
    public static final String SOURCE = "sensitive.ini";
    public static final String FIELDS = "Fields";
    public static final String PROCEDURES = "Procedures";
    public static final Set<String> CONFIG = Sets.newHashSet();

    static {
        HierarchicalINIConfiguration source = null;
        try {
            source = new HierarchicalINIConfiguration(SOURCE);
        } catch (ConfigurationException e) {
            logger.error("Load sensitive.ini error!", e);
            throw new RuntimeException("Load sensitive.ini error!", e);
        }

        for (String sectionName : new String[] {FIELDS, PROCEDURES}) {
            SubnodeConfiguration section = source.getSection(sectionName);
            Iterator<String> keys = section.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                List<Object> values = section.getList(key);
                if (CollectionUtils.isEmpty(values)) {
                    logger.warn("Key #{}# is empty!", key);
                    continue;
                }
                for(Object value : values) {
                    CONFIG.add(key + "." + value);
                }
            }
        }
    }

}
