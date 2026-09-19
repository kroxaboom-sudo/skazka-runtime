package com.kroxaboom.skazka.runtime;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * RU: Низкоуровневая проверка подписи не содержит встроенных production-ключей.
 * Trust roots передаёт конкретное приложение или сервис.
 *
 * EN: Low-level signature verification contains no embedded production keys.
 * Concrete applications or services provide their trust roots.
 */
public final class Ed25519Signatures {
    private Ed25519Signatures() {}

    public static boolean verify(byte[] payload, byte[] signature, byte[] publicKeySpki)
            throws Exception {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload must not be empty");
        }
        if (signature == null || signature.length != 64) {
            throw new IllegalArgumentException("Ed25519 signature must contain 64 bytes");
        }
        if (publicKeySpki == null || publicKeySpki.length == 0) {
            throw new IllegalArgumentException("Public key must not be empty");
        }

        PublicKey publicKey = publicKey(publicKeySpki);
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update(payload);
        return verifier.verify(signature);
    }

    private static PublicKey publicKey(byte[] spki) throws Exception {
        Exception first = null;
        for (String algorithm : new String[]{"Ed25519", "1.3.101.112"}) {
            try {
                return KeyFactory.getInstance(algorithm)
                        .generatePublic(new X509EncodedKeySpec(spki));
            } catch (Exception error) {
                if (first == null) {
                    first = error;
                }
            }
        }
        throw first;
    }
}
