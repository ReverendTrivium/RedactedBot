package org.redacted.Handlers.economy;

import com.google.gson.Gson;
import org.redacted.Database.json.EconomyResponses;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles localized responses to economy commands.
 *
 * @author Derrick Eberlein
 */
public class EconomyLocalization {

    private static final String PATH = "localization/economy.json";

    private final String[] work;
    private final String[] crimeSuccess;
    private final String[] crimeFail;

    /**
     * Reads economy.json responses into local memory
     */
    public EconomyLocalization() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PATH);
        Reader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
        EconomyResponses responses = new Gson().fromJson(reader, EconomyResponses.class);
        work = responses.getWork();
        crimeSuccess = responses.getCrimeSuccess();
        crimeFail = responses.getCrimeFail();
    }

    /**
     * Get a reply from the list of 'work' responses.
     *
     * @param amount the amount of money earned.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getWorkResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(work.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = work[index].replace("{amount}", value);
        return new EconomyReply(reply, index+1);
    }

    /**
     * Get a reply from the list of 'crimeSuccess' responses.
     *
     * @param amount the amount of money earned.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getCrimeSuccessResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(crimeSuccess.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = crimeSuccess[index].replaceAll("\\{amount}", value);
        return new EconomyReply(reply, index+1, true);
    }

    /**
     * Get a reply from the list of 'crimeFail' responses.
     *
     * @param amount the amount of money list.
     * @return an EconomyReply object with response and ID number.
     */
    public EconomyReply getCrimeFailResponse(long amount, String currency) {
        int index = ThreadLocalRandom.current().nextInt(crimeFail.length);
        String value = currency+" "+EconomyHandler.FORMATTER.format(amount);
        String reply = crimeFail[index].replaceAll("\\{amount}", value);
        return new EconomyReply(reply, index+1, false);
    }

    /**
     * Get a reply indicating the user avoided the fine by having no cash on hand.
     *
     * @return an EconomyReply object with a custom response and ID number.
     */
    public EconomyReply getSmartBankResponse() {
        String reply = "You were smart and put your money in the bank, so you avoided the fine!";
        return new EconomyReply(reply, 1, false); // ID can be 0 or some special value for this scenario
    }
}
