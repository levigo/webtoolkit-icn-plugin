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

    /** Upload endpoint of the jadice web toolkit addon-upload module (relative to the configured server URL). */
    private final static String JWT_UPLOAD_PATH = "upload";
    /** Prefix of the ICN document id that is stripped before the P8 ids are handed to jadice. */
    private final static String ICN_DOCUMENT_ID_PREFIX = "Document,";
    /** Default lifetime of a token (first open) when the admin panel leaves the field empty. */
    private final static long DEFAULT_TOKEN_TTL_MINUTES = 480L;
    private final static java.util.concurrent.atomic.AtomicBoolean TOKEN_SECRET_WARNED =
            new java.util.concurrent.atomic.AtomicBoolean();

    private static String stripDocumentPrefix(String docId) {
        if (docId != null && docId.startsWith(ICN_DOCUMENT_ID_PREFIX)) {
            return docId.substring(ICN_DOCUMENT_ID_PREFIX.length());
        }
        return docId;
    }

    private static long parseTtl(Object configured) {
        if (configured == null) {
            return DEFAULT_TOKEN_TTL_MINUTES;
        }
        try {
            long ttl = Long.parseLong(String.valueOf(configured).trim());
            return ttl > 0 ? ttl : DEFAULT_TOKEN_TTL_MINUTES;
        } catch (NumberFormatException e) {
            return DEFAULT_TOKEN_TTL_MINUTES;
        }
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
        // Get JWT endpoint from configuration
        String jwtServerURI;
        String renderQuality;
        String icnServerURI;
        boolean p8AnnotationsEnabled;
        String tokenSecret;
        long tokenTtlMinutes;
        try {
            JSONObject configuration = JSONObject.parse(callbacks.loadConfiguration());
            jwtServerURI = (String) configuration.get("jwtServerURI");
            icnServerURI = (String) configuration.get("icnServerURI");
            renderQuality = (String) configuration.get("renderQuality");
            p8AnnotationsEnabled = Boolean.TRUE.equals(configuration.get("p8AnnotationsEnabled"));
            tokenSecret = (String) configuration.get("tokenSecret");
            tokenTtlMinutes = parseTtl(configuration.get("tokenTtlMinutes"));
        } catch (final Exception e) {
            Logger.logError(this, "execute", "Couldn't load the configuration. ", e);
            this.sendErrorIFrame(response, "Couldn't load the configuration.");
            return;
        }
        if (jwtServerURI == null) {
            Logger.logError(this, "execute", request, "jadice web toolkit server url not configured");
            this.sendErrorIFrame(response, "jadice web toolkit server url not configured");
            return;
        }
        String serverType = request.getParameter("serverType");
        boolean p8Document = p8AnnotationsEnabled && "p8".equalsIgnoreCase(serverType);
        boolean tokenMode = p8Document && tokenSecret != null && !tokenSecret.trim().isEmpty();
        if (p8Document && !tokenMode && TOKEN_SECRET_WARNED.compareAndSet(false, true)) {
            Logger.logWarning(this, "execute", request, "P8 annotations are enabled but no token secret is "
                    + "configured; falling back to uploading the document (not cluster-safe)");
        }
        if (tokenMode) {
            // Token mode: no download, no upload. The signed token names the P8 document; jadice web toolkit
            // verifies it with the shared secret and reads content and annotations from P8 itself.
            String docId = stripDocumentPrefix(request.getParameter("docId"));
            String[] ids = docId == null ? null : docId.split(",");
            if (ids == null || ids.length != 2) {
                Logger.logError(this, "execute", request, "Unexpected P8 document id: " + docId);
                this.sendErrorIFrame(response, "Unexpected P8 document id");
                return;
            }
            boolean writable = "true".equals(request.getParameter("viewAnnotations"))
                    && "true".equals(request.getParameter("editAnnotations"));
            long expiresAt = System.currentTimeMillis() / 1000L + tokenTtlMinutes * 60L;
            String token = IcnToken.create(tokenSecret.trim(), ids[0], ids[1], writable, expiresAt);
            this.sendViewerIFrame(response, jwtServerURI, token, true);
            return;
        }

        // Upload mode (CM repositories, or P8 without token secret): download from ICN, upload to jadice.
        // this is the URL for the REST-Endpoint of CN to receive the document
        final String docUrlString = request.getParameter("docUrl");
        Logger.logDebug(this, "execute", request,
                "Downloading the following Document from ICN: " + docUrlString);
        URL docUrl = null;
        try {
            String base = (icnServerURI != null && !"".equals(icnServerURI)) ? icnServerURI : request.getRequestURL().toString();
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

        String p8Id = null;
        String contentType = request.getParameter("contentType");
        String viewAnnotations = request.getParameter("viewAnnotations");
        String editAnnotations = request.getParameter("editAnnotations");
        // The P8 id is only handed over when the admin enabled P8 annotations in the plugin configuration;
        // with it, jadice loads the document via icn:// (reads/writes FileNet annotations), without it via
        // upload:// (display only, no ICN integration module needed on the jadice side).
        if (p8AnnotationsEnabled && "p8".equalsIgnoreCase(serverType) && "true".equals(viewAnnotations)
                && "true".equals(editAnnotations)) {
            // Only transfer the P8-ID to jadice if it is allowed to view and edit annotations
            // if that is set to false, we don't transfer the ID so jadice won't be able to
            // load the annotations as the ID is missing
            p8Id = stripDocumentPrefix(request.getParameter("docId"));
        }

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
            this.sendErrorIFrame(response, "Not authenticated for ICN");
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
                // Send it to JWT (addon-upload, multipart). Only the P8 object id travels as meta
                // property; P8 credentials are configured in the jadice web toolkit
                // (webtoolkit.icn.p8.*) and never leave it.
                String url = jwtServerURI + (jwtServerURI.endsWith("/") ? "" : "/") + JWT_UPLOAD_PATH;
                docGeneratedId = sendMultipartPost(url, docStream, contentType, p8Id, request);
                assert !docGeneratedId.isEmpty();
                Logger.logDebug(this, "execute", request,
                        "Got the following ID from jadice: " + docGeneratedId);
            } catch (Exception exception) {
                Logger.logError(this, "execute", request,
                        "An error occurred while transferring the document to jadice", exception);
                this.sendErrorIFrame(response, "An error occurred while transferring the document");
                return;
            }
        } catch (IOException ioException) {
            Logger.logError(this, "execute", request,
                    "An error occurred while reading the document from ICN", ioException);
            this.sendErrorIFrame(response, "An error occurred while reading the document from ICN");
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
        this.sendViewerIFrame(response, jwtServerURI, docGeneratedId, p8Id != null);
    }

    /**
     * Sends the IFrames' content to the client: a redirect to the viewer with the document source as
     * {@code ?u=} parameter (JSON {@code {"uri": "<scheme>://<uploadId>"}}), the same parameter the viewer
     * accepts for any other URI source.
     *
     * @param response       The response stream.
     * @param jwtEndpointUrl The URI pointing to the <i>jadice web viewer</i> instance.
     * @param uploadId       The id returned by the upload endpoint, or the signed token in token mode.
     * @param p8Document     {@code true} when the document lives in P8 and annotations are to be read/written;
     *                       the viewer then loads {@code icn://} (needs the ICN integration on the server),
     *                       otherwise plain {@code upload://}.
     */
    private void sendViewerIFrame(HttpServletResponse response, String jwtEndpointUrl, String uploadId, boolean p8Document) {
        try {
            JSONObject source = new JSONObject();
            source.put("uri", (p8Document ? "icn://" : "upload://") + uploadId);
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

    /**
     * Sends the error message as the IFrames' content.
     *
     * @param response     The response stream.
     * @param errorMessage The error message to display.
     */
    private void sendErrorIFrame(HttpServletResponse response, String errorMessage) {
        try {
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
     * Uploads the document as {@code multipart/form-data} to the addon-upload endpoint of jadice web toolkit.
     * Form fields: {@code metaMapFlat} ({@code p8Id=<objectStoreId>,<documentId>}, only when present) and
     * {@code file} (the document bytes).
     *
     * @param url         the upload endpoint
     * @param inputStream the document content
     * @param contentType MIME type of the document as reported by ICN, may be {@code null}
     * @param p8Id        {@code <objectStoreId>,<documentId>} or {@code null}
     * @return the upload id returned by jadice web toolkit
     */
    private String sendMultipartPost(String url, InputStream inputStream, String contentType, String p8Id,
                                     HttpServletRequest request) throws Exception {
        String boundary = "----jwt-icn-" + UUID.randomUUID();
        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("POST");
        httpClient.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpClient.setDoOutput(true);
        httpClient.setChunkedStreamingMode(64 * 1024);

        int readBytes = 0;
        try (DataOutputStream target = new DataOutputStream(httpClient.getOutputStream())) {
            if (p8Id != null) {
                writeAscii(target, "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"metaMapFlat\"\r\n\r\n"
                        + "p8Id=" + p8Id + "\r\n");
            }
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
