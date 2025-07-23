package org.redacted.util.googleCalendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.Getter;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Utility class for interacting with the Google Calendar API using a service account.
 * This class initializes the Calendar service and provides methods to interact with it.
 */
@Getter
public class CalendarAPI {

    private static final String SERVICE_ACCOUNT_FILE = "service_account.json";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final Calendar service;

    /**
     * Initializes the CalendarAPI with the service account credentials.
     * This constructor sets up the Google Calendar service using the provided service account file.
     */
    public CalendarAPI() {
        try {
            var credentials = ServiceAccountCredentials.fromStream(new FileInputStream(SERVICE_ACCOUNT_FILE))
                    .createScoped(List.of(CalendarScopes.CALENDAR));

            this.service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("Redacted Bot")
                    .build();

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace(); // or log more cleanly
            throw new RuntimeException("Failed to initialize Google Calendar API with service account", e);
        }
    }
}
