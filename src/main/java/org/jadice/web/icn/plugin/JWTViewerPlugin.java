package org.jadice.web.icn.plugin;

import java.util.Locale;

import com.ibm.ecm.extension.PluginViewerDef;

/**
 * Provides an abstract class that is extended to define a viewer provided by
 * the plug-in. The viewer can be used in viewer mappings to identify the types
 * of documents for which the viewer should be invoked. Viewers are launched
 * within their own iframes or web browser windows, based on a URL that is built
 * by IBM Content Navigator by using a template that is defined in this class.
 */
public class JWTViewerPlugin extends PluginViewerDef {

    @Override
    public String getId() {
        return "JWTViewerPlugin";
    }

    @Override
    public String getName(Locale locale) {
        return "jadice web toolkit";
    }

    @Override
    public boolean isLaunchInSeparateWindow() {
        return false;
    }

    /**
     * Frameless viewer: the plugin's own widget ({@link #getViewerClass()}) asks the service for the viewer URL and
     * embeds it in an iframe it owns, so Content Navigator never touches a cross-origin frame. The former launch
     * URL pattern (Content Navigator's generic iframe viewer, same origin required) is kept here for reference:
     * {@code servicesUrl+'/plugin.do?plugin=JWTPlugin&action=jwtViewerPluginService&docUrl='+encodeURIComponent(docUrl)
     * +'&contentType='+mimeType+'&docId='+docId+'&targetContentType=application/pdf&serverType='+serverType+privs}
     */
    @Override
    public String getLaunchUrlPattern() {
        return "";
    }

    @Override
    public String getViewerClass() {
        return "JwtPluginDojo.JWTViewerPlugin";
    }

    @Override
    public String[] getSupportedContentTypes() {
        return new String[]{
                "application/pdf",
                "application/vnd.ibm.modcap",
                "application/afp",
                "image/bmp",
                "image/gif",
                "image/jpeg",
                "image/png",
                "image/tiff",
                "image/jp2"
        };
    }

    @Override
    public String[] getSupportedServerTypes() {
        return new String[]{"p8", "cm"};
    }
}
