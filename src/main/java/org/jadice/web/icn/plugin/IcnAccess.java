package org.jadice.web.icn.plugin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.filenet.api.constants.AccessRight;

/**
 * Helpers for the access check of the token mode: the plugin only signs a token after Content Navigator has
 * handed it the P8 document in the user's own security context.
 */
final class IcnAccess {

    private IcnAccess() {
    }

    /**
     * @param url  a URL or path with a query string, as ICN passes it in {@code docUrl}
     * @param name the parameter name
     * @return the decoded value of the first occurrence, or {@code null}
     */
    static String queryParam(String url, String name) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        if (q < 0) {
            return null;
        }
        for (String pair : url.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (key.equals(name)) {
                try {
                    return eq >= 0 ? URLDecoder.decode(pair.substring(eq + 1), "UTF-8") : "";
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }

    /**
     * FileNet grants annotating through the "Link a document / Annotate" right.
     *
     * @param accessAllowed the mask from {@code Document.getAccessAllowed()} for the current user
     * @return whether the user may create annotations on the document
     */
    static boolean mayAnnotate(int accessAllowed) {
        return (accessAllowed & AccessRight.LINK_AS_INT) != 0;
    }
}
