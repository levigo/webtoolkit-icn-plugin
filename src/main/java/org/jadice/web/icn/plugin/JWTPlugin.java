package org.jadice.web.icn.plugin;

import com.ibm.ecm.extension.Plugin;
import com.ibm.ecm.extension.PluginAction;
import com.ibm.ecm.extension.PluginFeature;
import com.ibm.ecm.extension.PluginLayout;
import com.ibm.ecm.extension.PluginMenu;
import com.ibm.ecm.extension.PluginMenuType;
import com.ibm.ecm.extension.PluginODAuthenticationService;
import com.ibm.ecm.extension.PluginOpenAction;
import com.ibm.ecm.extension.PluginRequestFilter;
import com.ibm.ecm.extension.PluginResponseFilter;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.extension.PluginViewerDef;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides the main class of an IBM Content Navigator plug-in. The abstract
 * methods provide the name and version of the plug-in, and identify the
 * components such as actions, services, and scripts that are included in the
 * plug-in. The name of the <code>Plugin</code> subclass is specified in the
 * plug-in JAR file by using the following property in the manifest:
 *
 * <pre>
 *     Plugin-Class: <i>pluginClassName</i>
 * </pre>
 * <p>
 * The location of the plug-in JAR file is specified in the IBM Content
 * Navigator <code>AppConfig.xml</code> file. An administrator can edit this
 * file by adding the plug-in to the IBM Content Navigator configuration by
 * using the IBM Content Navigator administration tool.
 * <p>
 */
public class JWTPlugin extends Plugin {

    /**
     * Initializes this plug-in in the web client. A plug-in can perform
     * initialization that would be shared for all users of the application in
     * the method. This method differs from the constructor because this method
     * is called only when the plug-in is used within the web application. This
     * method is invoked when the application is initializing. This is an
     * optional method and may be removed from the plug-in if no applicationInit
     * is required.
     */
    @Override
    public void applicationInit(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
    }

    /**
     * Provides an identifier for the plug-in.
     * <p>
     * <strong>Important:</strong> This identifier is used in path names and
     * URLs so it must contain only alphanumeric characters.
     * </p>
     */
    @Override
    public String getId() {
        return "JWTPlugin";
    }

    /**
     * Provides a descriptive name for this plug-in. The name identifies the
     * plug-in in the IBM Content Navigator administration tool
     *
     * @parm locale The locale currently in use. The name should be returned in
     * the language for this locale if it is translated.
     */
    @Override
    public String getName(Locale locale) {
        return "jadice web toolkit plugin";
    }

    /**
     * Provides a version indicator for this plug-in. The version information is
     * displayed in the administration tool to help the administrator validate
     * the version of the plug-in that is being used. The plug-in writer needs
     * to update this indicator when redistributing a modified version of the
     * plug-in.
     *
     * @return A <code>String</code> representation of the version indicator for
     * the plug-in.
     */
    @Override
    public String getVersion() {
        return "1.1.0";
    }

    /**
     * Provides the copyright license for this plug-in. The information is
     * displayed in the About dialog Plugins tab. The license is provided by the
     * plugin creator. This is an optional method and may be removed from the
     * plug-in if no copyright is specified.
     *
     * @return A <code>String</code> representation of the license for the
     * plug-in.
     * @since 2.0.2
     */
    @Override
    public String getCopyright() {
        return "copyright levigo solutions GmbH";
    }

    /**
     * Returns the name of a JavaScript file provided by this plug-in. This file
     * is downloaded to the web browser during the initialization of the web
     * client, before login. A script can be used to perform customization that
     * cannot be performed by other extension mechanisms, such as features or
     * layouts. However, it is preferable to use these other extension
     * mechanisms to provide more flexibility to the administrator when
     * configuring desktops.
     */
    @Override
    public String getScript() {
        return "JWTPlugin.js";
    }

    /**
     * Returns the name of a debug version of the JavaScript file provided by
     * getScript(). The default implementation invokes getScript().
     *
     * @since 2.0.2
     */
    @Override
    public String getDebugScript() {
        return getScript();
    }

    /**
     * Returns the name of a Dojo module or widget that is contained in the
     * resources for this plug-in. IBM Content Navigator performs the necessary
     * <code>dojo.registerModulePath</code> mapping to allow modules or widgets
     * with mapped path names to be loaded by using the
     * <code>dojo.require</code> method. A specified module can be the directory
     * or package name for a set of Dojo modules or widgets.
     */
    @Override
    public String getDojoModule() {
        return "JwtPluginDojo";
    }

    /**
     * Returns the name of a CSS file that contains styles for this plug-in. IBM
     * Content Navigator generates the necessary style tag to pull in this file
     * when IBM Content Navigator loads the plug-in.
     */
    @Override
    public String getCSSFileName() {
        return "JWTPlugin.css";
    }

    /**
     * Returns a debug version of the CSS file returned by getCSSFileName. The
     * default implementation invokes getCSSFileName.
     *
     * @return
     * @since 2.0.2
     */
    @Override
    public String getDebugCSSFileName() {
        return getCSSFileName();
    }

    /**
     * Provides a list of actions that this plug-in adds to the main toolbar of
     * the web client.
     *
     * @return An array of <code>{@link com.ibm.ecm.extension.PluginAction
     * PluginAction}</code> objects. The plug-in should return the same
     * set of objects on every call.
     */
    @Override
    public PluginAction[] getActions() {
        return new PluginAction[0];
    }

    /**
     * Provides a list of open actions that this plug-in provides for supported
     * items.
     *
     * @return An array of <code>{@link com.ibm.ecm.extension.PluginOpenAction
     * PluginOpenAction}</code> objects. The plug-in should return the
     * same set of objects on every call.
     * @since 2.0.2
     */
    @Override
    public PluginOpenAction[] getOpenActions() {
        return new PluginOpenAction[0];
    }

    /**
     * Provides a list of filters that are run before a requested service. This
     * method can be used to modify the request or block the request.
     *
     * @return An array of <code>
     * {@link com.ibm.ecm.extension.PluginRequestFilter
     * PluginRequestFilter}</code> objects.
     */
    @Override
    public PluginRequestFilter[] getRequestFilters() {
        return new PluginRequestFilter[0];
    }

    /**
     * Provides a list of filters that are run after a requested service. This
     * list of filters can be used to modify the response that is returned.
     *
     * @return An array of <code>
     * {@link com.ibm.ecm.extension.PluginResponseFilter
     * PluginResponseFilter}</code> objects.
     */
    @Override
    public PluginResponseFilter[] getResponseFilters() {
        return new PluginResponseFilter[0];
    }

    /**
     * Provides a custom service used for Content Manager OnDemand single
     * sign-on (SSO). This is an optional service that will be called when SSO
     * is enabled on a Content Manager OnDemand server. The result of the
     * service will be the information passed through the Content Manager
     * OnDemand Web Enablement Kit "passThru" API.
     *
     * @return A {@link com.ibm.ecm.extension.PluginODAuthenticationService
     * PluginODAuthenticationService} object used as an authentication
     * exit for Content Manager OnDemand single sign-on.
     * @since 2.0.2
     */
    @Override
    public PluginODAuthenticationService getODAuthenticationService() {
        return null;
    }

    /**
     * Returns the name of a Dojo <code>dijit</code> class that provides a
     * configuration interface widget for this plug-in. The widget must extend
     * the <code>ecm.widget.admin.PluginConfigurationPane</code> widget. An
     * instance of the widget is created and displayed in the IBM Content
     * Navigator administration tool for configuration that is specific to the
     * plug-in.
     * <p>
     * Refer to the documentation on
     * {@link ecm.widget.admin.PluginConfigurationPane PluginConfigurationPane}
     * for more information on what is required for a plug-in configuration user
     * interface.
     * </p>
     */
    @Override
    public String getConfigurationDijitClass() {
        return "JwtPluginDojo.ConfigurationPane";
    }

    /**
     * Specifies one or more custom layouts that are provided by this plug-in.
     * Custom layouts can display the features and other user interface
     * components of IBM Content Navigator in a different arrangement. The IBM
     * Content Navigator administration tool is used to select the layout to use
     * for a desktop.
     *
     * @return An array of plug-in layout objects.
     */
    @Override
    public PluginLayout[] getLayouts() {
        return new PluginLayout[0];
    }

    /**
     * Specifies custom features that are provided by this plug-in. Features are
     * the major user interface sections, which appear as icons on the left side
     * of the user interface in the default layout. Examples of features include
     * Search, Favorites, and Teamspaces.
     *
     * @return An array of custom plug-in feature objects.
     */
    @Override
    public PluginFeature[] getFeatures() {
        return new PluginFeature[0];
    }

    /**
     * Provides a list of new menu types defined by the plug-in.
     *
     * @return An array of new menu type objects.
     */
    @Override
    public PluginMenuType[] getMenuTypes() {
        return new PluginMenuType[0];
    }

    /**
     * Provides a list of menus defined by the plug-in.
     *
     * @return An array of plug-in menu objects.
     */
    @Override
    public PluginMenu[] getMenus() {
        return new PluginMenu[0];
    }

    /**
     * Provides a list of viewers that are provided by this plug-in. The viewers
     * become available in the viewer configuration area of the IBM Content
     * Navigator administration tool. The viewers can be mapped to be used to
     * view certain document types.
     * <p>
     * <strong>Note:</strong> Typically, a plug-in does not define multiple
     * viewers. However, this method can be used to provide multiple
     * configurations of the same viewer, such as a view-only version and an
     * editing mode version of the same viewer.
     * </p>
     *
     * @return An array of {@link ecm.widget.admin.PluginViewerDef
     * PluginViewerDef} objects describing the viewers provided by the
     * plug-in.
     */
    @Override
    public PluginViewerDef[] getViewers() {
        return new PluginViewerDef[]{new JWTViewerPlugin()};
    }

    /**
     * Provides a list of services that are provided by this plug-in. The
     * services run on the web server, and can be called by the web browser
     * logic component of the plug-in.
     *
     * @return An array of {@link com.ibm.ecm.extension.PluginService
     * PluginService} objects. The plug-in should return the same set of
     * objects on every call. If there are no services defined by the
     * plug-in, the call should return an empty array.
     */
    @Override
    public PluginService[] getServices() {
        return new PluginService[]{new JWTViewerPluginService()};
    }
}
