package org.redacted.Commands.Color;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ColorCyclerPride class is responsible for cycling through a predefined set of colors
 * representing the Pride Flag. It changes the color of a specified role in a Discord server
 * at regular intervals.
 *
 * @author Derrick Eberlein
 */
public class ColorCyclerPride {
    private static final List<String> COLORS = new ArrayList<>();
    private int currentIndex = 0;

    static {
        // Add your 6 desired colors to the list
        COLORS.add("#E40303"); // Red
        COLORS.add("#FF8C00"); // Orange
        COLORS.add("#FFED00"); // Yellow
        COLORS.add("#008026"); // Green
        COLORS.add("#40e0d0"); // Turquoise
        COLORS.add("#fb607f"); // Bright Pink
    }

    /**
     * Cycles through the predefined colors and returns the next color.
     *
     * @return The next color in the cycle as a Color object.
     */
    public Color cycleColor() {
        String color = COLORS.get(currentIndex);
        currentIndex = (currentIndex + 1) % COLORS.size();
        return Color.decode(color);
    }

    /**
     * Starts the color cycling process for a specified role in a Discord guild.
     *
     * @param guild The Discord guild where the role exists.
     * @param interval The time interval in milliseconds for changing the color.
     * @param roleID The ID of the role whose color will be changed.
     */
    public void changeColorsPride(Guild guild, long interval, String roleID) {
        // Set Variables
        System.out.println("[ Info ] Starting color cycling for role ID: " + roleID + " in server: " + guild.getName());

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Role role = guild.getRoleById(roleID);
                if (role == null) {
                    System.out.println("[ Error ] Didn't Find Any Role, Server Name: " + guild.getName());
                    return;
                }

                System.out.println("[ Info ] Cycling color for role: " + role.getName());

                if (interval < 60000) {
                    System.out.println("\n[!!!] Enjoy Your Pride Roles");
                }

                // Cycle through Pride Flag Colors
                Color nextColor = cycleColor();
                role.getManager().setColor(nextColor).queue(
                        success -> System.out.println("Pride Role color changed successfully to " + nextColor.toString() + "."),
                        failure -> System.out.println("[ Error ] An error occurred during the role color change.")
                );
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, interval);
    }
}


