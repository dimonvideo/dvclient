package com.dimonvideo.client.util.pm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Creates a non-secret account identity without putting credentials into work requests. */
public final class PmDeletionAccount {
    /** Prevents instantiation of account identity helpers. */
    private PmDeletionAccount() { }

    /** Returns an account-bound SHA-256 identity, or null for an incomplete login. */
    public static String key(String login, int userId) {
        if (login == null || login.trim().length() < 2 || userId <= 0) return null;
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                    (userId + ":" + login.trim().toLowerCase(Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(Character.forDigit((value >>> 4) & 15, 16));
                result.append(Character.forDigit(value & 15, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Android", exception);
        }
    }
}
