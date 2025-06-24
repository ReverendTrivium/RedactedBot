package org.redacted.Handlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ManualVerificationTracker Class
 * This class tracks manual verifications for users who need to be verified manually.
 * It stores user data such as user ID, first name, Instagram handle, and Facebook handle.
 * It allows adding pending verifications and retrieving/removing them by message ID.
 *
 * @author Derrick Eberlein
 */
public class ManualVerificationTracker {
    // Maps message ID ➝ VerificationData for users needing manual verification
    private static final Map<Long, VerificationData> pendingVerifications = new ConcurrentHashMap<>();

    /**
     * Represents the data required for manual verification of a user.
     */
    public static class VerificationData {
        public final long userId;
        public final String firstName;
        public final String instagramHandle;
        public final String facebookHandle;

        /**
         * Constructs a VerificationData object with the provided user information.
         *
         * @param userId           The ID of the user to be verified.
         * @param firstName        The first name of the user.
         * @param instagramHandle  The Instagram handle of the user.
         * @param facebookHandle   The Facebook handle of the user.
         */
        public VerificationData(long userId, String firstName, String instagramHandle, String facebookHandle) {
            this.userId = userId;
            this.firstName = firstName;
            this.instagramHandle = instagramHandle;
            this.facebookHandle = facebookHandle;
        }
    }

    /**
     * Adds a pending verification for a user.
     *
     * @param messageId       The ID of the message associated with the verification.
     * @param userId          The ID of the user to be verified.
     * @param firstName       The first name of the user.
     * @param ig              The Instagram handle of the user.
     * @param fb              The Facebook handle of the user.
     */
    public static void addPendingVerification(long messageId, long userId, String firstName, String ig, String fb) {
        pendingVerifications.put(messageId, new VerificationData(userId, firstName, ig, fb));
    }

    /**
     * Retrieves and removes the verification data for a given message ID.
     *
     * @param messageId The ID of the message to retrieve verification data for.
     * @return The VerificationData associated with the message ID, or null if not found.
     */
    public static VerificationData getVerification(long messageId) {
        return pendingVerifications.remove(messageId);
    }

    /**
     * Checks if there is a pending verification for the given message ID.
     *
     * @param messageId The ID of the message to check.
     * @return True if there is a pending verification, false otherwise.
     */
    public static boolean isPending(long messageId) {
        return pendingVerifications.containsKey(messageId);
    }
}