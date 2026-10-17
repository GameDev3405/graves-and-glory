package gg.gravesandglory;

/** Compatibility only: this is not authentication or proof of an unmodified client. */
public final class CompatibilityPolicy {
    public static final int PROTOCOL = 1;
    private CompatibilityPolicy() {}

    public static String required(String version, String build) {
        return "This server requires Graves and Glory " + version + " (build "
                + build.substring(0, Math.min(8, build.length()))
                + "). Install the same modpack release as the server, then reconnect.";
    }

    public static String failure(String version, String build, int protocol, String clientVersion, String clientBuild) {
        if (protocol == PROTOCOL && version.equals(clientVersion) && build.equals(clientBuild)) return "";
        return "Your Graves and Glory pack does not match this server. " + required(version, build);
    }
}
