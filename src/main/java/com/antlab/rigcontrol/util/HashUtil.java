package com.antlab.rigcontrol.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class HashUtil {
    private HashUtil() {
    }

    public static String sha256String(String input) {
        try {
            return hex(sha256(input.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha256File(Path path) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static byte[] sha256(byte[] input) throws IOException {
        MessageDigest digest = newDigest();
        return digest.digest(input);
    }

    private static MessageDigest newDigest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
