package org.redacted.Roles;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 * Function to get all the roles by their name in a guild.
 *
 * @author Derrick Eberlein
 */
public class getRolesByName {

    public Role getRoleByName(Guild guild, String roleName) {
        for (Role role : guild.getRoles()) {
            if (role.getName().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }
}
