package org.redacted.util.SocialMedia.Reddit;

import org.bson.Document;
import org.redacted.Database.Database;

import java.time.Instant;

/**
 * Manages Reddit OAuth tokens, checking for validity and refreshing them as needed.
 * Uses a Database instance to store and retrieve tokens.
 *
 * @author Derrick Eberlein
 */
public class RedditTokenManager {
    private final Database database;
    private final RedditOAuth redditOAuth;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;

    /**
     * Constructs a RedditTokenManager with the provided database and RedditOAuth instance.
     *
     * @param database the database instance to store and retrieve tokens
     * @param redditOAuth the RedditOAuth instance for authentication
     * @param clientId the Reddit application client ID
     * @param clientSecret the Reddit application client secret
     * @param username the Reddit username
     * @param password the Reddit password
     */
    public RedditTokenManager(Database database, RedditOAuth redditOAuth, String clientId, String clientSecret, String username, String password) {
        this.database = database;
        this.redditOAuth = redditOAuth;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
    }

    /**
     * Retrieves a valid Reddit OAuth token, refreshing it if necessary.
     *
     * @return the valid token as a String, or null if unable to fetch a new token
     */
    public String getValidToken() {
        Document tokenDoc = database.getRedditToken();
        if (tokenDoc != null) {
            // Check if the token is still valid
            Instant expiration = tokenDoc.getDate("expiration").toInstant();
            System.out.println("Current token expires at: " + expiration);
            System.out.println("Time Now: " + Instant.now());
            if (Instant.now().isBefore(expiration)) {
                return tokenDoc.getString("token");
            }
            else {
                System.out.println("Token expired, fetching a new one.");
                try {
                    String token = redditOAuth.authenticate(clientId, clientSecret, username, password);
                    Instant newExpiration = Instant.now().plusSeconds(3600); // 1 hour
                    System.out.println("New token expires at: " + newExpiration);
                    database.clearRedditToken();
                    database.storeRedditToken(token, newExpiration);
                    return token;
                } catch (Exception e) {
                    System.err.println("[RedditTokenManager] Failed to fetch new token: " + e.getMessage());
                    return null;
                }
            }
        }

        return null;
    }
}