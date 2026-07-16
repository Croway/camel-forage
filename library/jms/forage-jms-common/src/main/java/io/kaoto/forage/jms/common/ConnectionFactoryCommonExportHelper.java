package io.kaoto.forage.jms.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for transforming JMS kind identifiers into provider class names.
 */
public class ConnectionFactoryCommonExportHelper {
    private static final Map<String, String> JMS_KIND_TO_PROVIDER_CLASS = new HashMap<>();

    static {
        JMS_KIND_TO_PROVIDER_CLASS.put("artemis", "io.kaoto.forage.jms.artemis.ArtemisJms");
        JMS_KIND_TO_PROVIDER_CLASS.put("ibmmq", "io.kaoto.forage.jms.ibmmq.IbmMqJms");
    }

    public static String transformJmsKindIntoProviderClass(String jmsKind) {
        String providerClass = JMS_KIND_TO_PROVIDER_CLASS.get(jmsKind.toLowerCase());
        if (providerClass == null) {
            throw new IllegalArgumentException(
                    "Unknown JMS kind '%s'. Valid options: %s".formatted(jmsKind, JMS_KIND_TO_PROVIDER_CLASS.keySet()));
        }
        return providerClass;
    }
}
