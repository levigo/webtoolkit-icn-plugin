package org.jadice.web.icn.plugin;

import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.serviceability.Logger;
import com.ibm.json.java.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides an abstract class that is extended to create a class implementing
 * each service provided by the plug-in. Services are actions, similar to
 * servlets or Struts actions, that perform operations on the IBM Content
 * Navigator server. A service can access content server application programming
 * interfaces (APIs) and Java EE APIs.
 * <p>
 * Services are invoked from the JavaScript functions that are defined for the
 * plug-in by using the <code>ecm.model.Request.invokePluginService</code>
 * function.
 * </p>
 * Follow best practices for servlets when implementing an IBM Content Navigator
 * plug-in service. In particular, always assume multi-threaded use and do not
 * keep unshared information in instance variables.
 */
public class JWTViewerPluginService extends PluginService {

    /** Upload endpoint of the jadice web toolkit addon-upload module (relative to the configured viewer URL). */
    private final static String JWT_UPLOAD_PATH = "upload";

    /**
     * ICN item ids look like {@code <className>,{objectStoreGuid},{documentGuid}}; the class name is
     * {@code Document} for the base class and the custom class name otherwise.
     *
     * @return {@code {objectStoreId, documentId}} (the last two parts), or {@code null} when there are fewer
     */
    static String[] p8IdsOf(String docId) {
        if (docId == null) {
            return null;
        }
        String[] parts = docId.split(",");
        if (parts.length < 2) {
            return null;
        }
        return new String[]{parts[parts.length - 2].trim(), parts[parts.length - 1].trim()};
    }

    /**
     * The user's access mask on the fetched document; when FileNet cannot provide it (property not fetched by
     * ICN), the ICN privilege alone decides and a warning is logged.
     */
    private boolean mayAnnotate(com.filenet.api.core.Document fetched, HttpServletRequest request) {
        try {
            return IcnAccess.mayAnnotate(fetched.getAccessAllowed());
        } catch (RuntimeException e) {
            Logger.logWarning(this, "execute", request, "Could not read the access mask of the P8 document, "
                    + "relying on the ICN privilege for the edit right: " + e.getMessage());
            return true;
        }
    }

    /**
     * The ICN user for the token's {@code sub}: {@code PluginServiceCallbacks.getUserId()} when ICN provides it
     * (it throws for some service invocation paths), otherwise the container's remote user, otherwise none.
     */
    private String userIdOf(PluginServiceCallbacks callbacks, HttpServletRequest request) {
        try {
            String userId = callbacks.getUserId();
            if (userId != null && !userId.trim().isEmpty()) {
                return userId;
            }
        } catch (RuntimeException e) {
            Logger.logDebug(this, "execute", request, "callbacks.getUserId() not available: " + e);
        }
        try {
            String remoteUser = request.getRemoteUser();
            if (remoteUser == null && request.getUserPrincipal() != null) {
                remoteUser = request.getUserPrincipal().getName();
            }
            return remoteUser;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Exception class, message and the first stack frame of this plugin, for the error shown in ICN. */
    private static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder(e.toString());
        for (StackTraceElement frame : e.getStackTrace()) {
            if (frame.getClassName().startsWith("org.jadice.")) {
                sb.append(" at ").append(frame.getClassName()).append('.').append(frame.getMethodName())
                        .append(':').append(frame.getLineNumber());
                break;
            }
        }
        if (e.getStackTrace().length > 0 && !sb.toString().contains(" at ")) {
            StackTraceElement top = e.getStackTrace()[0];
            sb.append(" at ").append(top.getClassName()).append('.').append(top.getMethodName())
                    .append(':').append(top.getLineNumber());
        }
        return sb.toString();
    }

    /**
     * Returns the unique identifier for this service.
     * <p>
     * <strong>Important:</strong> This identifier is used in URLs so it must
     * contain only alphanumeric characters.
     * </p>
     *
     * @return A <code>String</code> that is used to identify the service.
     */
    @Override
    public String getId() {
        return "jwtViewerPluginService";
    }

    /**
     * Performs the action of this service.
     *
     * @param callbacks An instance of the <code>PluginServiceCallbacks</code> class
     *                  that contains several functions that can be used by the
     *                  service. These functions provide access to the plug-in
     *                  configuration and content server APIs.
     * @param request   The <code>HttpServletRequest</code> object that provides the
     *                  request. The service can access the invocation parameters from
     *                  the request.
     * @param response  The <code>HttpServletResponse</code> object that is generated
     *                  by the service. The service can get the output stream and
     *                  write the response. The response must be in JSON format.
     * @throws Exception For exceptions that occur when the service is running. If the
     *                   logging level is high enough to log errors, information about
     *                   the exception is logged by IBM Content Navigator.
     */
    @Override
    public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) {
        try {
            doExecute(callbacks, request, response);
        } catch (RuntimeException e) {
            Logger.logError(this, "execute", request, "Unexpected failure", e);
            this.sendError(request, response, "jadice web viewer could not be opened: " + describe(e));
        }
    }

    private void doExecute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) {
        PluginConfig config;
        try {
            config = PluginConfig.of(JSONObject.parse(callbacks.loadConfiguration()));
        } catch (final Exception e) {
            Logger.logError(this, "execute", "Couldn't load the configuration. ", e);
            this.sendError(request, response, "Couldn't load the configuration.");
            return;
        }
        String invalid = config.validate();
        if (invalid != null) {
            Logger.logError(this, "execute", request, "Plugin configuration invalid: " + invalid);
            this.sendError(request, response, invalid);
            return;
        }
        String viewerUrl = config.getViewerUrl();
        String serverType = request.getParameter("serverType");
        boolean tokenMode = config.usesToken(serverType);
        if (config.isP8AnnotationsEnabled() && !tokenMode) {
            Logger.logDebug(this, "execute", request, "P8 annotations are enabled but the repository type is '"
                    + serverType + "'; opening the document without annotations via upload");
        }
        if (tokenMode) {
            // Token mode: no download, no upload. The signed token names the P8 document; jadice web toolkit
            // verifies it with the shared secret and reads content and annotations from P8 itself.
            // Access check: ICN fetches the document named by docId in the user's own security context; only
            // then is a token signed for exactly that id. The edit right additionally needs the user's
            // FileNet Link/Annotate right on the document (when the access mask is available).
            try {
                String docUrlForIds = request.getParameter("docUrl");
                String repositoryId = IcnAccess.queryParam(docUrlForIds, "repositoryId");
                String vsId = IcnAccess.queryParam(docUrlForIds, "vsId");
                String rawDocId = request.getParameter("docId");
                String[] ids = p8IdsOf(rawDocId);
                if (ids == null || repositoryId == null) {
                    Logger.logError(this, "execute", request, "Missing P8 document id or repository id");
                    this.sendError(request, response, "Unexpected P8 document id");
                    return;
                }
                com.filenet.api.core.Document fetched;
                try {
                    fetched = callbacks.getP8Document(repositoryId, rawDocId, vsId == null ? "" : vsId, "");
                } catch (Exception e) {
                    Logger.logError(this, "execute", request, "Access check for the P8 document failed", e);
                    fetched = null;
                }
                if (fetched == null) {
                    this.sendError(request, response, "Not authorized to open this document");
                    return;
                }
                boolean icnEdit = "true".equals(request.getParameter("viewAnnotations"))
                        && "true".equals(request.getParameter("editAnnotations"));
                boolean writable = icnEdit && mayAnnotate(fetched, request);
                long expiresAt = System.currentTimeMillis() / 1000L + config.getTokenTtlMinutes() * 60L;
                String token = IcnToken.create(config.getTokenSecret(), ids[0], ids[1], writable, expiresAt,
                        userIdOf(callbacks, request));
                this.sendViewer(request, response, viewerUrl, token, true);
            } catch (RuntimeException e) {
                Logger.logError(this, "execute", request, "Token mode failed", e);
                this.sendError(request, response, "jadice web viewer could not be opened: " + describe(e));
            }
            return;
        }

        // Upload mode (P8 annotations disabled, or a non-P8 repository): download from ICN, upload to jadice.
        // The viewer shows the document via upload://, without any P8 annotations.
        // this is the URL for the REST-Endpoint of CN to receive the document
        final String docUrlString = request.getParameter("docUrl");
        Logger.logDebug(this, "execute", request,
                "Downloading the following Document from ICN: " + docUrlString);
        URL docUrl = null;
        try {
            String base = config.getIcnServerUrl() != null ? config.getIcnServerUrl() : request.getRequestURL().toString();
            docUrl = new URL(new URL(base), docUrlString);
        } catch (MalformedURLException malformedURLException) {
            Logger.logError(this, "execute", "MalformedURLException. ", malformedURLException);
        }
        // Example of request parameters
        // plugin: JWTPlugin
        // action: jwtViewerPluginService
        // docUrl: /navigator/jaxrs/p8/getDocument?docid=Document%2C%7B0BB25229-CD00-4A03-B374-AAD214ACC365%7D%2C%7BA853C638-2A0A-4D67-82CC-4E360A43F03B%7D&template_name=Document&repositoryId=P8Repository&vsId=%7B60A7AE78-0000-C714-9EE8-8F6D7DF7BFF0%7D&objectStoreName=P8ObjectStore&security_token=-3642916214142016050
        // contentType: application/pdf
        // docId: Document,{0BB25229-CD00-4A03-B374-AAD214ACC365},{A853C638-2A0A-4D67-82CC-4E360A43F03B}
        // targetContentType: application/pdf
        // serverType: p8
        // printDoc: true
        // exportDoc: true
        // viewAnnotations: true
        // editAnnotations: true
        // editDoc: true
        // editProperties: true
        // security_token: -3642916214142016050
        // desktop: jadice

        String contentType = request.getParameter("contentType");

        // Copy the cookies so the call is authenticated
        Cookie[] cookies = request.getCookies();
        StringBuilder cookieProperty = new StringBuilder();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieProperty.append(cookie.getName());
                cookieProperty.append("=");
                cookieProperty.append(cookie.getValue());
                cookieProperty.append(",");
            }
        } else {
            Logger.logError(this, "execute", request, "No cookies present");
            this.sendError(request, response, "Not authenticated for ICN");
            return;
        }

        HttpURLConnection docConnection = null;
        String docGeneratedId;
        try {
            InputStream docStream;
            assert docUrl != null;
            docConnection = (HttpURLConnection) docUrl.openConnection();
            docConnection.setRequestProperty("COOKIE", cookieProperty.toString());
            docStream = docConnection.getInputStream();
            try {
                // Send it to the viewer (addon-upload, multipart). Nothing but the bytes travels.
                String url = viewerUrl + (viewerUrl.endsWith("/") ? "" : "/") + JWT_UPLOAD_PATH;
                docGeneratedId = sendMultipartPost(url, docStream, contentType, request);
                assert !docGeneratedId.isEmpty();
                Logger.logDebug(this, "execute", request,
                        "Got the following ID from jadice: " + docGeneratedId);
            } catch (Exception exception) {
                Logger.logError(this, "execute", request,
                        "An error occurred while transferring the document to jadice", exception);
                this.sendError(request, response, "An error occurred while transferring the document");
                return;
            }
        } catch (IOException ioException) {
            Logger.logError(this, "execute", request,
                    "An error occurred while reading the document from ICN", ioException);
            this.sendError(request, response, "An error occurred while reading the document from ICN");
            return;
        } finally {
            if (docConnection != null) {
                try {
                    docConnection.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
        // Display the JWT-viewer in the iframe
        this.sendViewer(request, response, viewerUrl, docGeneratedId, false);
    }

    /**
     * Sends the IFrames' content to the client: a redirect to the viewer with the document source as
     * {@code ?u=} parameter (JSON {@code {"uri": "<scheme>://<uploadId>"}}), the same parameter the viewer
     * accepts for any other URI source.
     *
     * @param response       The response stream.
     * @param jwtEndpointUrl The URI pointing to the <i>jadice web viewer</i> instance.
     * @param uploadId       The id returned by the upload endpoint, or the signed token in token mode.
     * @param tokenMode      {@code true} for a signed token: the viewer loads {@code icn://} (reads/writes P8
     *                       annotations, needs the ICN integration on the server), otherwise plain
     *                       {@code upload://} (display only).
     */
    private void sendViewer(HttpServletRequest request, HttpServletResponse response, String jwtEndpointUrl,
                            String uploadId, boolean tokenMode) {
        try {
            JSONObject source = new JSONObject();
            source.put("uri", (tokenMode ? "icn://" : "upload://") + uploadId);
            if (wantsJson(request)) {
                // called by the plugin's own viewer widget (JwtPluginDojo.JWTViewerPlugin), which puts the URL
                // into an iframe it owns; no same-origin requirement on the ICN side
                String url = jwtEndpointUrl + "?connection=longpoll&theme=icn&u=" + URLEncoder.encode(source.serialize(), "UTF-8");
                JSONObject json = new JSONObject();
                json.put("url", url);
                writeJson(response, json);
                return;
            }
            response.setContentType("text/html; charset=utf-8");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<html lang='en'><body>");
            stringBuilder.append("<p>Loading jadice web viewer...</p>");
            stringBuilder.append("<script>location.href = '");
            stringBuilder.append(jwtEndpointUrl);
            stringBuilder.append("?connection=longpoll&theme=icn&u=");
            stringBuilder.append(URLEncoder.encode(source.serialize(), "UTF-8"));
            stringBuilder.append("';</script>");
            stringBuilder.append("</body></html>");
            response.getOutputStream().write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            Logger.logError(this, "execute", "Failed to write the viewer redirect to the HttpServletResponse.", e);
        }
    }

    private static boolean wantsJson(HttpServletRequest request) {
        return "json".equals(request.getParameter("format"));
    }

    private static void writeJson(HttpServletResponse response, JSONObject json) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.getOutputStream().write(json.serialize().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends the error message as the IFrames' content.
     *
     * @param response     The response stream.
     * @param errorMessage The error message to display.
     */
    private void sendError(HttpServletRequest request, HttpServletResponse response, String errorMessage) {
        try {
            if (wantsJson(request)) {
                JSONObject json = new JSONObject();
                json.put("error", errorMessage);
                writeJson(response, json);
                return;
            }
            response.setContentType("text/html");
            final PrintWriter writer = response.getWriter();
            writer.append("<html><body>");
            writer.append("<span style=\"color: #000; font-family: sans-serif;\">").append(errorMessage).append("</span>");
            writer.append("</body></html>");
        } catch (final IOException e) {
            Logger.logError(this, "execute", "Failed to get the PrintWriter from the HttpServletResponse.", e);
        }
    }

    /**
     * Uploads the document as {@code multipart/form-data} to the addon-upload endpoint of the viewer. The only
     * form field is {@code file} (the document bytes); no P8 ids travel, upload mode shows the document without
     * annotations.
     *
     * @param url         the upload endpoint
     * @param inputStream the document content
     * @param contentType MIME type of the document as reported by ICN, may be {@code null}
     * @return the upload id returned by the viewer
     */
    private String sendMultipartPost(String url, InputStream inputStream, String contentType,
                                     HttpServletRequest request) throws Exception {
        String boundary = "----jwt-icn-" + UUID.randomUUID();
        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("POST");
        httpClient.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpClient.setDoOutput(true);
        httpClient.setChunkedStreamingMode(64 * 1024);

        int readBytes = 0;
        try (DataOutputStream target = new DataOutputStream(httpClient.getOutputStream())) {
            writeAscii(target, "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"document\"\r\n"
                    + "Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n\r\n");
            byte[] buf = new byte[8192];
            int length;
            while ((length = inputStream.read(buf)) > 0) {
                target.write(buf, 0, length);
                readBytes += length;
            }
            writeAscii(target, "\r\n--" + boundary + "--\r\n");
            target.flush();
        }
        Logger.logDebug(this, "sendMultipartPost", request, "Transferred " + readBytes + " bytes to jadice");

        int status = httpClient.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Upload to jadice web toolkit failed with HTTP " + status);
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString().trim();
    }

    private static void writeAscii(DataOutputStream target, String text) throws IOException {
        target.write(text.getBytes(StandardCharsets.US_ASCII));
    }

}
