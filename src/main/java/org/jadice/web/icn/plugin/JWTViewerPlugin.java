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

    @Override
    public String getLaunchUrlPattern() {
        return "servicesUrl+'/plugin.do?plugin=JWTPlugin&action=jwtViewerPluginService&docUrl='+encodeURIComponent(docUrl)+'&contentType='+mimeType+'&docId='+docId+'&targetContentType=application/pdf&serverType='+serverType+privs";
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
