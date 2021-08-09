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

    private final static String JWT_DOCUMENT_URL = "jwt/documentupload";

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
        String p8Uri;
        String p8User;
        String p8Password;
        String icnServerURI;
        try {
            jwtServerURI = (String) JSONObject.parse(callbacks.loadConfiguration()).get("jwtServerURI");
            icnServerURI = (String) JSONObject.parse(callbacks.loadConfiguration()).get("icnServerURI");
            renderQuality = (String) JSONObject.parse(callbacks.loadConfiguration()).get("renderQuality");
            p8Uri = (String) JSONObject.parse(callbacks.loadConfiguration()).get("p8Uri");
            p8User = (String) JSONObject.parse(callbacks.loadConfiguration()).get("p8User");
            p8Password = (String) JSONObject.parse(callbacks.loadConfiguration()).get("p8Password");
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
        String viewAnnotations = request.getParameter("viewAnnotations");
        String editAnnotations = request.getParameter("editAnnotations");
        String serverType = request.getParameter("serverType");
        if ("p8".equalsIgnoreCase(serverType) && "true".equals(viewAnnotations) && "true".equals(editAnnotations)) {
            // Only transfer the P8-ID to jadice if it is allowed to view and edit annotations
            // if that is set to false, we don't transfer the ID so jadice won't be able to
            // load the annotations as the ID is missing
            p8Id = request.getParameter("docId");
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
                // Send it to JWT
                String url = jwtServerURI + (jwtServerURI.endsWith("/") ? "" : "/") + JWT_DOCUMENT_URL;
                if (p8Id != null) {
                    String p8Encoded = URLEncoder.encode(p8Id, StandardCharsets.UTF_8.toString());
                    url += "?p8Id=" + p8Encoded;
                    if (p8Uri != null && p8User != null && p8Password != null) {
                        String userEncoded = URLEncoder.encode(p8User, StandardCharsets.UTF_8.toString());
                        String passwordEncoded = URLEncoder.encode(p8Password, StandardCharsets.UTF_8.toString());
                        String uriEncoded = URLEncoder.encode(p8Uri, StandardCharsets.UTF_8.toString());
                        url += "&p8Uri=" + uriEncoded;
                        url += "&p8User=" + userEncoded;
                        url += "&p8Password=" + passwordEncoded;
                    }
                }
                docGeneratedId = sendPost(url, docStream, request);
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
        this.sendViewerIFrame(response, jwtServerURI, docGeneratedId, renderQuality);
    }

    /**
     * Sends the IFrames' content to the client via the given
     * {@link HttpServletResponse}. This includes a call to the <i>jadice web
     * toolkit</i> instance as a REST request.
     *
     * @param response       The response stream.
     * @param jwtEndpointUrl The URI pointing to the <i>jadice webtool kit</i> instance.
     * @param token          The token identifying the the document.
     * @param renderQuality  The render-quality for web-toolkit
     */
    private void sendViewerIFrame(HttpServletResponse response, String jwtEndpointUrl, String token, String renderQuality) {
        try {
            response.setContentType("text/html; charset=utf-8");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<html lang='en'><body>");
            stringBuilder.append("<p>Loading jadice web toolkit...</p>");
            stringBuilder.append("<script>location.href = '");
            stringBuilder.append(jwtEndpointUrl);
            stringBuilder.append("?connection=longpoll&");
            if (renderQuality != null && !"".equals(renderQuality)) {
                stringBuilder.append("quality=");
                stringBuilder.append(renderQuality);
                stringBuilder.append("&");
            }
            stringBuilder.append("t=");
            stringBuilder.append(URLEncoder.encode(token, "UTF-8"));
            stringBuilder.append("';</script>");
            stringBuilder.append("</body></html>");
            response.getOutputStream().write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            Logger.logError(this, "execute", "Failed to get the PrintWriter from the HttpServletResponse.", e);
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
     * Takes the data from the inputStream and transfers it via HTTP-POST to the url
     *
     * @param url         the url to send the data to
     * @param inputStream the input-stream to copy the data from
     * @return the response of the POST-request
     */
    private String sendPost(String url, InputStream inputStream, HttpServletRequest request) throws Exception {
        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("POST");

        httpClient.setDoOutput(true);
        int readBytes = 0;
        try (DataOutputStream target = new DataOutputStream(httpClient.getOutputStream())) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = inputStream.read(buf)) > 0) {
                target.write(buf, 0, length);
                readBytes += length;
            }
            target.flush();
        }
        Logger.logDebug(this, "sendPost", request, "Transferred " + readBytes + "bytes to jadice");

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

}
