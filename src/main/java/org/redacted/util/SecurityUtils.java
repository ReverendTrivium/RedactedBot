package org.redacted.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Util methods for securing URLs
 *
 * @author Derrick Eberlein
 */
public class SecurityUtils {

    private static final List<String> ALLOWED_PROTOCOLS = List.of("http", "https");
    private static final List<String> ALLOWED_DOMAINS = List.of("youtube", "soundcloud", "twitch", "spotify", "apple");

    /**
     * Check if the given url is a whitelisted domain and protocol.
     * @param urlString The url to check. Can be malformed/invalid
     * @return True if the url given is whitelisted
     */
    public static boolean isUrlWhitelisted(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        // Check if the protocol is allowed
        System.out.println("Checking URL: " + url);
        boolean isValidProtocol = ALLOWED_PROTOCOLS.contains(url.getProtocol());
        String host = url.getHost();
        if(host.equals("youtu.be")) {
            return true;
        }
        else {
            System.out.println("Host: " + host);
        }
        String domain = getDomain(host);
        boolean isValidDomain = ALLOWED_DOMAINS.contains(domain);
        return isValidProtocol && isValidDomain;
    }

    /**
     * Return the domain of the given host
     * @param host The host url. Ex: www.youtube.com
     * @return The domain name without subdomains. Ex: YouTube
     */
    public static String getDomain(String host) {
        String[] parts = host.split("(\\.|%2E)"); // Match dot or URL-Encoded dot
        int size = parts.length;
        if(size == 1) {
            return host;
        }
        return parts[size - 2];
    }

}
