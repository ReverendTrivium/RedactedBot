package org.redacted.Handlers.economy;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import com.mongodb.lang.Nullable;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Economy;
import org.redacted.util.embeds.EmbedUtils;
import org.redacted.Database.cache.Config;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * EconomyHandler Class
 * This class manages the economy system for a Discord guild, including user balances,
 * bank accounts, and various economy-related commands such as work, crime, and rob.
 * It interacts with the database to store and retrieve user economy data.
 *
 * @author Derrick Eberlein
 */
@Getter
public class EconomyHandler {

    public static final String DEFAULT_CURRENCY = "\uD83E\uDE99";
    public static final long WORK_TIMEOUT = 14400000;
    public static final long ROB_TIMEOUT = 86400000;
    public static final DecimalFormat FORMATTER = new DecimalFormat("#,###");


    private static final UpdateOptions UPSERT = new UpdateOptions().upsert(true);
    private final Map<Long, UserTimeout> timeouts;
    private static final EconomyLocalization responses = new EconomyLocalization();

    private final Guild guild;
    private final GuildData guildData; // Add reference to GuildData
    private String currency;

    /**
     * Constructs an EconomyHandler for a specific guild.
     *
     * @param guild the guild this handler is for.
     * @param guildData the GuildData instance containing economy data.
     */
    public EconomyHandler(Guild guild, GuildData guildData) {
        this.guild = guild;
        this.guildData = guildData;

        // Retrieve config and ensure it is not null
        Config config = guildData.getConfig();
        if (config == null) {
            throw new IllegalStateException("Config for guild " + guild.getIdLong() + " is not initialized.");
        }

        // Debugging information
        System.out.println("Config details: " + config);
        System.out.println("Config currency: " + config.getCurrency());

        // Ensure currency is initialized
        this.currency = config.getCurrency();
        if (this.currency == null) {
            this.currency = DEFAULT_CURRENCY;
            System.out.println("Currency for guild " + guild.getIdLong() + " is null. Setting to default: " + DEFAULT_CURRENCY);

            // Update config in the database with default currency
            config.setCurrency(DEFAULT_CURRENCY);
            guildData.updateConfig(Updates.set("currency", DEFAULT_CURRENCY));
        }

        // Initialize the timeouts map
        this.timeouts = new HashMap<>();
    }

    /**
     * Work for money.
     * 1 in 3 chance to earn 200-1200 of the currency.
     *
     * @param userID the ID of user to work for.
     * @return an EconomyReply object with response, ID number, and success boolean.
     */
    public EconomyReply work(long userID) {
        int amount = ThreadLocalRandom.current().nextInt(1000) + 200;
        addMoney(userID, amount);
        setTimeout(userID, TIMEOUT_TYPE.WORK); // Set work timeout

        return new EconomyReply("You earned " + amount + " " + currency, 1, true);
    }

    /**
     * 40% chance to add 750-2750 to user's balance.
     * 60% chance to lose 20%-40% of user's cash balance.
     *
     * @param userID the ID of user whose balance to add to.
     * @return an EconomyReply object with response, ID number, and success boolean.
     */
    public EconomyReply crime(long userID) {
        long amount;
        EconomyReply reply;

        // Check if crime is successful
        if (ThreadLocalRandom.current().nextInt(100) <= 40) {
            // Crime successful
            amount = ThreadLocalRandom.current().nextInt(2000) + 750;
            addMoney(userID, amount);
            reply = responses.getCrimeSuccessResponse(amount, getCurrency());
        } else {
            // Crime failed
            amount = calculateFine(userID); // Calculate the fine (e.g., 20%-40% of the balance)

            // Get the current cash balance (non-banked money) of the user
            long currentBalance = getBalance(userID);

            if (currentBalance > 0) {
                // Deduct money only up to the available balance
                long amountToDeduct = Math.min(amount, currentBalance);
                removeMoney(userID, amountToDeduct); // Deduct the calculated fine
                reply = responses.getCrimeFailResponse(amountToDeduct, getCurrency());
            } else {
                // User has no cash on hand, return special response
                reply = responses.getSmartBankResponse(); // Use the new response for avoiding fine
            }
        }

        setTimeout(userID, TIMEOUT_TYPE.CRIME);
        return reply;
    }

    /**
     * Deposit money from balance into the bank.
     *
     * @param userID the ID of the user to deposit for.
     * @param amount the amount to deposit.
     */
    public void deposit(long userID, long amount) {
        long currentBalance = getBalance(userID);

        // Ensure the user has enough balance to deposit
        if (currentBalance < amount) {
            throw new IllegalArgumentException("Not enough balance to deposit.");
        }

        // Update the user's balance and bank in the database
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        Bson updateBalance = Updates.inc("balance", -amount);  // Decrease balance
        Bson updateBank = Updates.inc("bank", amount);         // Increase bank
        guildData.getEconomyCollection().updateOne(filter, Updates.combine(updateBalance, updateBank), UPSERT);
    }

    /**
     * Withdraw money from the bank into balance.
     *
     * @param userID the ID of the user to withdraw for.
     * @param amount the amount to withdraw.
     */
    public void withdraw(long userID, long amount) {
        long currentBank = getBank(userID);

        // Ensure the user has enough in the bank to withdraw
        if (currentBank < amount) {
            throw new IllegalArgumentException("Not enough funds in the bank to withdraw.");
        }

        // Update the user's balance and bank in the database
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        Bson updateBalance = Updates.inc("balance", amount);   // Increase balance
        Bson updateBank = Updates.inc("bank", -amount);        // Decrease bank
        guildData.getEconomyCollection().updateOne(filter, Updates.combine(updateBalance, updateBank), UPSERT);
    }


    /**
     * Add money to this user's account
     *
     * @param amount the amount of money to add.
     */
    public void addMoney(long userID, long amount) {
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        guildData.getEconomyCollection().updateOne(filter, Updates.inc("balance", amount), UPSERT);
    }

    /**
     * Remove money to this user's account
     *
     * @param amount the amount of money to remove.
     */
    public void removeMoney(long userID, long amount) {
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        guildData.getEconomyCollection().updateOne(filter, Updates.inc("balance", -amount), UPSERT);
    }

    /**
     * Get a user's current cash balance.
     *
     * @param userID the ID of the user to get cash balance from.
     * @return the integer value of user's cash balance.
     */
    public long getBalance(long userID) {
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        Economy profile = guildData.getEconomyCollection().find(filter).first();
        if (profile == null) return 0;
        return profile.getBalance() != null ? profile.getBalance() : 0;
    }

    /**
     * Get a user's current bank balance.
     *
     * @param userID the ID of the user to get bank balance from.
     * @return the integer value of user's bank balance.
     */
    public long getBank(long userID) {
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        Economy profile = guildData.getEconomyCollection().find(filter).first();
        if (profile == null) return 0;
        return profile.getBank() != null ? profile.getBank() : 0;
    }

    /**
     * Get a user's current networth (balance and bank added together).
     *
     * @param userID the ID of the user to get networth from.
     * @return the integer value of user's networth.
     */
    public long getNetworth(long userID) {
        long balance = getBalance(userID);
        long bank = getBank(userID);
        return balance + bank;
    }

    /**
     * Get a user's current bank balance.
     *
     * @param userID the ID of the user to get bank balance from.
     * @return the integer value of user's bank balance.
     */
    public Economy getProfile(long userID) {
        Bson filter = Filters.and(Filters.eq("user", userID), Filters.eq("guild", guild.getIdLong()));
        return guildData.getEconomyCollection().find(filter).first();
    }

    /**
     * Set a user timeout for a specific economy command.
     *
     * @param userID the user to set timeout for.
     * @param type the economy command to timeout.
     */
    private void setTimeout(long userID, TIMEOUT_TYPE type) {
        long time = System.currentTimeMillis() + WORK_TIMEOUT;
        UserTimeout userTimeout = timeouts.get(userID);
        if (userTimeout == null) {
            userTimeout = new UserTimeout(userID);
        }
        switch(type) {
            case WORK -> userTimeout.setWorkTimeout(time);
            case CRIME -> userTimeout.setCrimeTimeout(time);
            case ROB -> userTimeout.setRobTimeout(System.currentTimeMillis() + ROB_TIMEOUT);
        }
        timeouts.put(userID, userTimeout);
    }

    /**
     * Get a user's timeout for a specific economy command.
     *
     * @param userID the user to get the timeout from.
     * @param type the economy command that is timed out.
     * @return time in millis till timeout is up. Null if not set.
     */
    public @Nullable Long getTimeout(long userID, TIMEOUT_TYPE type) {
        UserTimeout userTimeout = timeouts.get(userID);
        if (userTimeout == null) {
            return null;
        }
        Long timeout = null;
        switch(type) {
            case WORK -> timeout = userTimeout.getWorkTimeout();
            case CRIME -> timeout = userTimeout.getCrimeTimeout();
            case ROB -> timeout = userTimeout.getRobTimeout();
        }
        return timeout;
    }

    /**
     * Formats timeout timestamp into a string timestamp for embeds.
     *
     * @param timeout the timestamp in millis.
     * @return a string timeout formatted for embeds.
     */
    public @Nullable String formatTimeout(long timeout) {
        return TimeFormat.RELATIVE.format(timeout);
    }

    /**
     * Attempt to steal another user's cash.
     *
     * @param userID the user attempting the robbery.
     * @param targetID the target being robbed.
     * @return an EconomyReply object with a response and success boolean.
     */
    public EconomyReply rob(long userID, long targetID) {
        // Calculate probability of failure (your networth / (their cash + your networth))
        long userNetworth = getNetworth(userID);
        long targetCash = getBalance(targetID);
        double failChance = (double) userNetworth / (targetCash + userNetworth);
        if (failChance < 0.20) {
            failChance = 0.20;
        } else if (failChance > 0.80) {
            failChance = 0.80;
        }

        // Calculate amount stolen (success probability * their cash)
        long amountStolen = (long) ((1 - failChance) * targetCash);
        if (amountStolen < 0) amountStolen = 0;

        // Attempt robbery
        setTimeout(userID, TIMEOUT_TYPE.ROB);
        if (ThreadLocalRandom.current().nextDouble() > failChance) {
            // Rob successful
            pay(targetID, userID, amountStolen);
            String value = getCurrency() + " " + EconomyHandler.FORMATTER.format(amountStolen);
            String response = EmbedUtils.GREEN_TICK + " You robbed " + value + " from <@" + targetID + ">";
            return new EconomyReply(response, 1, true);
        }
        // Rob failed (20-40% fine of net worth)
        long fine = calculateFine(userID);
        removeMoney(userID, fine);
        String value = getCurrency() + " " + EconomyHandler.FORMATTER.format(fine);
        String response = "You were caught attempting to rob <@"+targetID+">, and have been fined " + value + ".";
        return new EconomyReply(response, 1, false);
    }

    /**
     * Transfer money from one user to another.
     *
     * @param userID the user to transfer money from.
     * @param targetID the user to transfer money to.
     * @param amount the amount of money to transfer.
     */
    public void pay(long userID, long targetID, long amount) {
        removeMoney(userID, amount);
        addMoney(targetID, amount);
    }

    /**
     * Calculate fine for commands like /crime and /rob
     * Default fine is 20-40% of user's networth.
     *
     * @param userID the user to calculate fine for.
     * @return the calculated fine amount.
     */
    private long calculateFine(long userID) {
        long networth = getNetworth(userID);
        long fine = 0;
        if (networth > 0) {
            double percent = (ThreadLocalRandom.current().nextInt(20) + 20) * 0.01;
            fine = (long) (networth * percent);
        }
        return fine;
    }

    /**
     * Gets the rank of the specified user in their guild based on balance and bank.
     *
     * @param userID the ID of the user to get rank for.
     * @return integer ranking on this server.
     */
    public int getRank(long userID) {
        int rank = 1;
        for (Economy profile : getLeaderboard()) {
            if (profile.getUser() == userID) return rank;
            rank++;
        }
        return guild.getMemberCount();
    }

    /**
     * Get a sorted list of user economy data sorted by net worth.
     * This method transforms the AggregateIterable from MongoDB into a List<Economy>.
     *
     * @return list of Economy objects sorted by net worth in descending order.
     */
    public List<Economy> getLeaderboardAsList() {
        List<Economy> leaderboard = new ArrayList<>();
        AggregateIterable<Economy> leaderboardAggregate = getLeaderboard();

        try (MongoCursor<Economy> cursor = leaderboardAggregate.iterator()) {
            while (cursor.hasNext()) {
                leaderboard.add(cursor.next());
            }
        }

        return leaderboard;
    }

    /**
     * Get a sorted list of user economy data sorted by networth.
     *
     * @return iterable of user economy data sorted in descending order.
     */
    public AggregateIterable<Economy> getLeaderboard() {
        return guildData.getEconomyCollection().aggregate(
                Arrays.asList(
                        Aggregates.match(Filters.eq("guild", guild.getIdLong())),
                        Aggregates.addFields(new Field<>("sum", new Document("$add", Arrays.asList("$balance", new Document("$ifNull", Arrays.asList("$bank", 0)))))),
                        Aggregates.sort(Sorts.descending("sum"))
                )
        );
    }


    /**
     * Sets the currency symbol to a custom emoji or string.
     *
     * @param symbol the string or emoji symbol.
     */
    public void setCurrency(String symbol) {
        // Update in-memory Config object
        guildData.getConfig().setCurrency(symbol);

        // Update the database with the new currency symbol
        Bson update = Updates.set("currency", symbol);
        guildData.updateConfig(update);

        // Update the EconomyHandler's currency field
        this.currency = symbol;
    }


    /**
     * Resets the currency to the default value.
     * This will update the in-memory Config object, the database, and the EconomyHandler's currency field.
     */
    public void resetCurrency() {
        // Update in-memory Config object
        guildData.getConfig().setCurrency(EconomyHandler.DEFAULT_CURRENCY);

        // Update the database to unset the currency (or set to default)
        Bson update = Updates.unset("currency");
        guildData.updateConfig(update);

        // Update the EconomyHandler's currency field
        this.currency = EconomyHandler.DEFAULT_CURRENCY;
    }

    /**
     * Enum representing different types of timeouts for economy commands.
     */
    public enum TIMEOUT_TYPE {
        WORK, CRIME, ROB
    }
}
