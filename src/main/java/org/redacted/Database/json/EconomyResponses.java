package org.redacted.Database.json;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents list of responses to economy commands.
 * Used by OkHttp and Gson to convert JSON to java code.
 *
 * @author Derrick Eberlein
 */
@Getter
@Setter
public class EconomyResponses {

    private final String[] work;
    private final String[] crimeSuccess;
    private final String[] crimeFail;

    /**
     * Constructs an EconomyResponses object with specified responses.
     *
     * @param work          Array of work responses.
     * @param crimeSuccess  Array of successful crime responses.
     * @param crimeFail     Array of failed crime responses.
     */
    public EconomyResponses(String[] work, String[] crimeSuccess, String[] crimeFail) {
        this.work = work;
        this.crimeSuccess = crimeSuccess;
        this.crimeFail = crimeFail;
    }
}