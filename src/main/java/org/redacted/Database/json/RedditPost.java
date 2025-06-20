package org.redacted.Database.json;

import lombok.Getter;

/**
 * RedditPost Class
 * This class represents a Reddit post with its title, link, image URL, and upvotes.
 * It is used to store and retrieve information about Reddit posts.
 *
 * @author Derrick Eberlein
 */
@Getter
public class RedditPost {

    public static final String UPVOTE_EMOJI = "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/120/sony/336/thumbs-up_1f44d.png";
    private final String title;
    private final String postLink;
    private final String url;
    private final int ups;

    /**
     * Constructs a RedditPost object with the specified title, post link, image URL, and upvotes.
     *
     * @param title    The title of the Reddit post.
     * @param postLink The link to the Reddit post.
     * @param url      The URL of the image associated with the post.
     * @param ups      The number of upvotes the post has received.
     */
    public RedditPost(String title, String postLink, String url, int ups) {
        this.title = title;
        this.postLink = postLink;
        this.url = url;
        this.ups = ups;
    }
}
