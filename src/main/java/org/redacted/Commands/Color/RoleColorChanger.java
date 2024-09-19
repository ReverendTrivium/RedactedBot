package org.redacted.Commands.Color;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.Timer;
import java.util.TimerTask;

public class RoleColorChanger {

    public void changeColors(Guild guild, long interval, String roleID) {
        // Log the start of the color cycling process
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
                    System.out.println("\n[!!!] Enjoy Your Rainbow Roles." );
                }

                // Set random color excluding disallowed colors
                role.getManager().setColor(ColorGenerator.getRandomColor()).queue(
                        success -> System.out.println("Role color changed successfully"),
                        failure -> System.out.println("[ Error ] An error occurred during the role change.")
                );
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, interval);
    }
}

