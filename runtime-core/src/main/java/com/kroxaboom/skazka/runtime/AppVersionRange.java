package com.kroxaboom.skazka.runtime;

/**
 * RU: Диапазон совместимости runtime-конфигурации с версией приложения.
 * maxVersionCode == 0 означает отсутствие верхней границы.
 *
 * EN: Runtime-configuration compatibility range for an application version.
 * maxVersionCode == 0 means there is no upper bound.
 */
public record AppVersionRange(long minVersionCode, long maxVersionCode) {
    public AppVersionRange {
        if (minVersionCode < 1
                || maxVersionCode < 0
                || (maxVersionCode > 0 && maxVersionCode < minVersionCode)) {
            throw new IllegalArgumentException("Invalid application version range");
        }
    }

    public boolean supports(long installedVersionCode) {
        if (installedVersionCode < 1) {
            return false;
        }
        return installedVersionCode >= minVersionCode
                && (maxVersionCode == 0 || installedVersionCode <= maxVersionCode);
    }
}
