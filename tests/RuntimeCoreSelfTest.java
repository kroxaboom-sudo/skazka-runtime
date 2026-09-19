import com.kroxaboom.skazka.runtime.AppVersionRange;
import com.kroxaboom.skazka.runtime.AtomicRuntimeStore;
import com.kroxaboom.skazka.runtime.Ed25519Signatures;
import com.kroxaboom.skazka.runtime.HttpsEndpointPolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

public final class RuntimeCoreSelfTest {
    public static void main(String[] args) throws Exception {
        AppVersionRange range = new AppVersionRange(10, 20);
        check(range.supports(10), "minimum app version");
        check(range.supports(20), "maximum app version");
        check(!range.supports(9), "below minimum");
        check(!range.supports(21), "above maximum");
        check(new AppVersionRange(10, 0).supports(1000), "open upper bound");

        check(
                "https://api.example.org/v1".equals(
                        HttpsEndpointPolicy.normalize("https://api.example.org/v1/", true)
                ),
                "HTTPS endpoint normalization"
        );

        boolean httpRejected = false;
        try {
            HttpsEndpointPolicy.normalize("http://api.example.org", true);
        } catch (IllegalArgumentException expected) {
            httpRejected = true;
        }
        check(httpRejected, "HTTP endpoint rejected");

        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] payload = "runtime-payload".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(payload);
        byte[] signature = signer.sign();

        check(
                Ed25519Signatures.verify(payload, signature, keyPair.getPublic().getEncoded()),
                "Ed25519 signature"
        );

        byte[] modified = payload.clone();
        modified[0] ^= 1;
        check(
                !Ed25519Signatures.verify(modified, signature, keyPair.getPublic().getEncoded()),
                "modified payload rejected"
        );

        Path directory = Files.createTempDirectory("skazka-runtime-store-");
        try {
            AtomicRuntimeStore store = new AtomicRuntimeStore(directory, 1024);
            AtomicRuntimeStore.Verifier verifier = RuntimeCoreSelfTest::revision;

            byte[] builtin = bytes(1);
            check(store.load(builtin, verifier).revision() == 1, "builtin revision");
            check(store.install(builtin, bytes(2), verifier), "install revision 2");
            check(store.load(builtin, verifier).revision() == 2, "load revision 2");
            check(!store.install(builtin, bytes(2), verifier), "same revision ignored");
            check(store.install(builtin, bytes(3), verifier), "install revision 3");

            Files.writeString(directory.resolve("current.bin"), "broken", StandardCharsets.UTF_8);
            check(store.load(builtin, verifier).revision() == 2, "rollback to previous revision");
            check(store.load(builtin, verifier).revision() == 2, "rollback restored current");
        } finally {
            deleteTree(directory);
        }

        System.out.println("PASS: Skazka Runtime trust, compatibility, HTTPS and rollback core");
    }

    private static byte[] bytes(long revision) {
        return ("revision:" + revision).getBytes(StandardCharsets.UTF_8);
    }

    private static long revision(byte[] payload) {
        String value = new String(payload, StandardCharsets.UTF_8);
        if (!value.startsWith("revision:")) {
            throw new IllegalArgumentException("invalid test payload");
        }
        return Long.parseLong(value.substring("revision:".length()));
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
