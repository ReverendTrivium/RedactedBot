package org.redacted.util.SocialMedia.Reddit;

import org.bson.Document;
import org.redacted.Database.Database;

import java.time.Instant;

/**
 * Manages Reddit token storage and refreshing using the Database instance.
 */
public class RedditTokenManager {
    private final Database database;
    private final RedditOAuth redditOAuth;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;

    public RedditTokenManager(Database database, RedditOAuth redditOAuth, String clientId, String clientSecret, String username, String password) {
        this.database = database;
        this.redditOAuth = redditOAuth;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
    }

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