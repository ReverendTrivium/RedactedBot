package org.redacted.Roles;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 * Function to get all the roles by their name in a guild.
 *
 * @author Derrick Eberlein
 */
public class getRolesByName {

    /**
     * Retrieves a role from the specified guild by its name.
     *
     * @param guild    The guild from which to retrieve the role.
     * @param roleName The name of the role to retrieve.
     * @return The Role object if found, or null if no matching role is found.
     */
    public Role getRoleByName(Guild guild, String roleName) {
        for (Role role : guild.getRoles()) {
            if (role.getName().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }
}
