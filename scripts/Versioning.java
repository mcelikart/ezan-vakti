import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** JDK-only version preparation, also invoked automatically by Gradle packaging. */
public class Versioning {
    private static final Pattern SEMVER = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");
    private static final int MAX_VERSION_CODE = 2_100_000_000;

    public static void main(String[] args) {
        try {
            if (args.length < 2 || args.length > 3 || !(args[0].equals("current") || args[0].equals("prepare"))) {
                throw new IllegalArgumentException("Usage: java scripts/Versioning.java current|prepare PROJECT_DIR [patch|minor|major]");
            }
            Path root = Path.of(args[1]).toAbsolutePath().normalize();
            String bump = args.length == 3 ? args[2] : "patch";
            if (!(bump.equals("patch") || bump.equals("minor") || bump.equals("major"))) {
                throw new IllegalArgumentException("versionBump must be patch, minor or major");
            }
            if (args[0].equals("current")) {
                print(read(root.resolve("version.properties")));
                return;
            }
            Path lockDirectory = root.resolve(".gradle");
            Files.createDirectories(lockDirectory);
            try (FileChannel channel = FileChannel.open(lockDirectory.resolve("versioning.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = channel.lock()) {
                Path versionFile = root.resolve("version.properties");
                Properties version = read(versionFile);
                String fingerprint = fingerprint(root);
                String previous = version.getProperty("sourceFingerprint", "").trim();
                if (!fingerprint.equals(previous)) {
                    // A new release can set its intended version before its first
                    // package. The empty baseline must not immediately bump it again.
                    if (!previous.isEmpty()) increment(version, bump);
                    version.setProperty("sourceFingerprint", fingerprint);
                    write(versionFile, version);
                }
                print(version);
            }
        } catch (Exception error) {
            System.err.println(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            System.exit(1);
        }
    }

    private static Properties read(Path file) throws Exception {
        Properties version = new Properties();
        try (InputStream input = Files.newInputStream(file)) { version.load(input); }
        String name = version.getProperty("versionName", "").trim();
        Matcher match = SEMVER.matcher(name);
        if (!match.matches()) throw new IllegalArgumentException("version.properties versionName must use MAJOR.MINOR.PATCH");
        for (int part = 1; part <= 3; part++) Integer.parseInt(match.group(part));
        int code = Integer.parseInt(version.getProperty("versionCode", "0").trim());
        if (code < 1 || code > MAX_VERSION_CODE) throw new IllegalArgumentException("Android versionCode is outside its supported range");
        version.setProperty("versionName", name);
        version.setProperty("versionCode", Integer.toString(code));
        return version;
    }

    private static void increment(Properties version, String bump) {
        String[] parts = version.getProperty("versionName").split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        if (bump.equals("major")) { major = Math.addExact(major, 1); minor = 0; patch = 0; }
        else if (bump.equals("minor")) { minor = Math.addExact(minor, 1); patch = 0; }
        else patch = Math.addExact(patch, 1);
        int code = Math.addExact(Integer.parseInt(version.getProperty("versionCode")), 1);
        if (code > MAX_VERSION_CODE) throw new IllegalArgumentException("Android versionCode limit reached");
        version.setProperty("versionName", major + "." + minor + "." + patch);
        version.setProperty("versionCode", Integer.toString(code));
    }

    private static String fingerprint(Path root) throws Exception {
        Map<String, Path> inputs = new TreeMap<>();
        for (String directory : new String[] {"app/src", "assets", "gradle", "scripts"}) {
            Path folder = root.resolve(directory);
            if (!Files.isDirectory(folder)) continue;
            try (Stream<Path> files = Files.walk(folder)) {
                files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !hasGeneratedSegment(root.relativize(path)))
                    .forEach(path -> inputs.put(relative(root, path), path));
            }
        }
        for (String filename : new String[] {"app/build.gradle.kts", "app/proguard-rules.pro",
                "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat"}) {
            Path path = root.resolve(filename);
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) inputs.put(filename, path);
        }
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        for (Map.Entry<String, Path> input : inputs.entrySet()) {
            hash.update(input.getKey().getBytes(StandardCharsets.UTF_8));
            hash.update((byte) 0);
            hash.update(Long.toString(Files.size(input.getValue())).getBytes(StandardCharsets.UTF_8));
            hash.update((byte) 0);
            try (InputStream bytes = Files.newInputStream(input.getValue())) {
                int count;
                while ((count = bytes.read(buffer)) != -1) hash.update(buffer, 0, count);
            }
            hash.update((byte) 0);
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : hash.digest()) hex.append(String.format("%02x", value & 0xff));
        return hex.toString();
    }

    private static boolean hasGeneratedSegment(Path relative) {
        for (Path segment : relative) {
            String name = segment.toString();
            if (name.equals("build") || name.equals(".gradle") || name.equals(".kotlin") ||
                    name.equals(".git") || name.equals("__pycache__")) return true;
        }
        return false;
    }

    private static String relative(Path root, Path file) { return root.relativize(file).toString().replace('\\', '/'); }

    private static void write(Path file, Properties version) throws Exception {
        String text = "# Canonical application version. Updated automatically when packaging changed sources.\n" +
            "versionName=" + version.getProperty("versionName") + "\n" +
            "versionCode=" + version.getProperty("versionCode") + "\n" +
            "sourceFingerprint=" + version.getProperty("sourceFingerprint") + "\n";
        Path temporary = Files.createTempFile(file.getParent(), ".version-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void print(Properties version) {
        System.out.println(version.getProperty("versionName") + "|" + version.getProperty("versionCode"));
    }
}
