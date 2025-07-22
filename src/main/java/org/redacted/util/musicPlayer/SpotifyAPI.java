package org.redacted.util.musicPlayer;

import org.apache.hc.core5.http.ParseException;
import org.redacted.Redacted;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;
import java.time.Instant;

/**
 * Spotify API client for authentication and access to Spotify's web API.
 * This class initializes the Spotify API client with client credentials.
 *
 * @author Derrick Eberlein
 */
public class SpotifyAPI {

    private final SpotifyApi spotifyApi;
    private Instant tokenExpiresAt;

    /**
     * Constructs a SpotifyAPI instance using the provided Redacted bot configuration.
     * This constructor initializes the SpotifyApi with client ID and secret from the bot's configuration.
     *
     * @param bot the Redacted bot instance containing configuration
     * @throws IOException if an I/O error occurs
     * @throws ParseException if the response cannot be parsed
     * @throws SpotifyWebApiException if the Spotify Web API returns an error
     */
    public SpotifyAPI(Redacted bot) throws IOException, ParseException, SpotifyWebApiException {
        String clientId = bot.config.get("SPOTIFY_CLIENT_ID");
        String clientSecret = bot.config.get("SPOTIFY_CLIENT_SECRET");

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();

        refreshToken(); // Fetch initial token
    }

    /**
     * Refreshes the Spotify API access token using client credentials.
     * This method retrieves a new access token and sets it in the SpotifyApi instance.
     *
     * @throws IOException if an I/O error occurs
     * @throws ParseException if the response cannot be parsed
     * @throws SpotifyWebApiException if the Spotify Web API returns an error
     */
    private void refreshToken() throws IOException, ParseException, SpotifyWebApiException {
        ClientCredentialsRequest credentialsRequest = spotifyApi.clientCredentials().build();
        ClientCredentials credentials = credentialsRequest.execute();

        spotifyApi.setAccessToken(credentials.getAccessToken());

        // Token expires in X seconds — record expiry time
        this.tokenExpiresAt = Instant.now().plusSeconds(credentials.getExpiresIn());
        System.out.println("🎧 [Spotify] Token refreshed, expires at: " + tokenExpiresAt);
    }

    /**
     * Returns the SpotifyApi instance, refreshing the token if it has expired.
     *
     * @return SpotifyApi instance
     */
    public SpotifyApi getSpotifyApi() {
        try {
            if (tokenExpiresAt == null || Instant.now().isAfter(tokenExpiresAt)) {
                refreshToken();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Optional: retry logic or fallback
        }

        return spotifyApi;
    }
}
