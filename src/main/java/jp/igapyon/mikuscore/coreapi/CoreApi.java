package jp.igapyon.mikuscore.coreapi;

/**
 * Minimal public core API placeholder for the straight-conversion foundation.
 */
public final class CoreApi {
    private CoreApi() {
    }

    public static String version() {
        Package pkg = CoreApi.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "0.1.0-SNAPSHOT" : version;
    }
}
