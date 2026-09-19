package com.kroxaboom.skazka.runtime;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * RU: Атомарное current/previous-хранилище для уже проверяемых runtime payload.
 * Оно не знает о JSON, transport или конкретном алгоритме подписи.
 *
 * EN: Atomic current/previous storage for runtime payloads that can be verified.
 * It has no knowledge of JSON, transport, or a particular signature algorithm.
 */
public final class AtomicRuntimeStore {
    private static final String CURRENT = "current.bin";
    private static final String PREVIOUS = "previous.bin";
    private static final String TEMP = "candidate.tmp";

    private final Path directory;
    private final int maxBytes;

    public AtomicRuntimeStore(Path directory, int maxBytes) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }
        if (maxBytes < 1) {
            throw new IllegalArgumentException("Payload size limit must be positive");
        }

        this.directory = directory;
        this.maxBytes = maxBytes;
    }

    public synchronized ActivePayload load(byte[] builtin, Verifier verifier) throws Exception {
        requireVerifier(verifier);
        ActivePayload builtInPayload = verified(builtin, verifier);

        Files.createDirectories(directory);
        Path current = directory.resolve(CURRENT);
        Path previous = directory.resolve(PREVIOUS);

        if (Files.isRegularFile(current)) {
            try {
                ActivePayload cached = verified(readLimited(current), verifier);
                if (cached.revision() > builtInPayload.revision()) {
                    return cached;
                }
            } catch (Exception brokenCurrent) {
                if (Files.isRegularFile(previous)) {
                    try {
                        ActivePayload rollback = verified(readLimited(previous), verifier);
                        if (rollback.revision() > builtInPayload.revision()) {
                            replace(previous, current, true);
                            return rollback;
                        }
                    } catch (Exception ignored) {
                        // RU: Если обе копии повреждены, immutable builtin остаётся последней безопасной точкой.
                        // EN: If both cached copies are broken, immutable builtin remains the final safe fallback.
                    }
                }
            }
        }

        return builtInPayload;
    }

    public synchronized boolean install(
            byte[] builtin,
            byte[] candidate,
            Verifier verifier
    ) throws Exception {
        requireVerifier(verifier);

        ActivePayload active = load(builtin, verifier);
        ActivePayload incoming = verified(candidate, verifier);
        if (incoming.revision() <= active.revision()) {
            return false;
        }

        Files.createDirectories(directory);
        Path current = directory.resolve(CURRENT);
        Path previous = directory.resolve(PREVIOUS);
        Path temp = directory.resolve(TEMP);

        writeSynced(temp, candidate);
        ActivePayload staged = verified(readLimited(temp), verifier);
        if (staged.revision() != incoming.revision()) {
            Files.deleteIfExists(temp);
            throw new IOException("Runtime payload changed while staging");
        }

        if (Files.isRegularFile(current)) {
            Files.copy(current, previous, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(previous);
        }

        replace(temp, current, false);
        try {
            ActivePayload installed = verified(readLimited(current), verifier);
            if (installed.revision() != incoming.revision()) {
                throw new IOException("Installed runtime revision does not match candidate");
            }
            return true;
        } catch (Exception installFailure) {
            if (Files.isRegularFile(previous)) {
                replace(previous, current, true);
            } else {
                Files.deleteIfExists(current);
            }
            throw new IOException("Runtime payload installation failed; rollback completed", installFailure);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private ActivePayload verified(byte[] bytes, Verifier verifier) throws Exception {
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw new IOException("Runtime payload size is invalid");
        }

        long revision = verifier.verify(bytes);
        if (revision < 1) {
            throw new IOException("Runtime payload revision must be positive");
        }
        return new ActivePayload(revision, bytes.clone());
    }

    private byte[] readLimited(Path file) throws IOException {
        long size = Files.size(file);
        if (size < 1 || size > maxBytes) {
            throw new IOException("Cached runtime payload size is invalid");
        }
        return Files.readAllBytes(file);
    }

    private static void writeSynced(Path file, byte[] data) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file.toFile(), false)) {
            output.write(data);
            output.flush();
            output.getFD().sync();
        }
    }

    private static void replace(Path source, Path destination, boolean copy) throws IOException {
        if (copy) {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (Exception atomicUnavailable) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void requireVerifier(Verifier verifier) {
        if (verifier == null) {
            throw new IllegalArgumentException("Verifier must not be null");
        }
    }

    @FunctionalInterface
    public interface Verifier {
        long verify(byte[] payload) throws Exception;
    }

    public record ActivePayload(long revision, byte[] bytes) {
        public ActivePayload {
            bytes = bytes == null ? new byte[0] : bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
