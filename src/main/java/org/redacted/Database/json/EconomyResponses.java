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

    public EconomyResponses(String[] work, String[] crimeSuccess, String[] crimeFail) {
        this.work = work;
        this.crimeSuccess = crimeSuccess;
        this.crimeFail = crimeFail;
    }
}