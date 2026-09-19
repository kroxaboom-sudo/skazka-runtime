package com.kroxaboom.skazka.runtime;

import java.net.URI;

/**
 * RU: Проверяет только структуру HTTPS endpoint. Список разрешённых hosts остаётся
 * политикой конкретного приложения и не зашивается в общий SDK.
 *
 * EN: Validates HTTPS endpoint structure only. The allowed-host list remains
 * application policy and is not embedded in the shared SDK.
 */
public final class HttpsEndpointPolicy {
    private HttpsEndpointPolicy() {}

    public static String normalize(String value, boolean allowPath) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            String path = uri.getRawPath();
            boolean rootPath = path == null || path.isEmpty() || "/".equals(path);

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawFragment() != null
                    || (!allowPath && !rootPath)) {
                throw new IllegalArgumentException("Endpoint must be a valid HTTPS URL");
            }

            String normalized = uri.toString();
            while (normalized.endsWith("/") && normalized.length() > "https://x".length()) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Endpoint must be a valid HTTPS URL", error);
        }
    }
}
