package com.antlab.systemthinker.sim;

public final class VersionUtil {
    private VersionUtil() {}

    public static String getVersionHash() {
        String sys = System.getProperty("systemthinker.version");
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        Package pkg = VersionUtil.class.getPackage();
        if (pkg != null) {
            String impl = pkg.getImplementationVersion();
            if (impl != null && !impl.isBlank()) {
                return impl.trim();
            }
        }
        return "unknown";
    }
}
