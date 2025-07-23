package org.redacted.util.moderation;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

/**
 * Service for managing roles in a Discord guild.
 * This service provides methods to remove all roles from a member
 * and restore previously removed roles.
 *
 * @author Derrick Eberlein
 */
public class RoleService {

    /**
     * Removes all roles from a member in the specified guild.
     *
     * @param guild The guild from which to remove roles.
     * @param member The member whose roles will be removed.
     * @return A list of roles that were removed.
     */
    public List<Role> removeAllRoles(Guild guild, Member member) {
        List<Role> oldRoles = member.getRoles();
        for (Role role : oldRoles) {
            guild.removeRoleFromMember(member, role).queue();
        }
        return oldRoles;
    }

    /**
     * Restores previously removed roles to a member in the specified guild.
     *
     * @param guild The guild in which to restore roles.
     * @param member The member to whom roles will be restored.
     * @param roleIds The IDs of the roles to restore.
     */
    public void restoreRoles(Guild guild, Member member, List<String> roleIds) {
        for (String roleId : roleIds) {
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(member, role).queue();
            }
        }
    }
}
