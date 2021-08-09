define([
        "dojo/_base/declare",
        "dojo/_base/lang",
        "ecm/widget/viewer/DocViewer",
        "dojo/text!./templates/JWTViewerPlugin.html",
        "ecm/LoggerMixin"
    ], //
    function (declare, lang, DocViewer, template, LoggerMixin) {

        return declare("JwtPluginDojo.JWTViewerPlugin", [
            DocViewer,
            LoggerMixin
        ], {
            templateString: template,

            /**
             * Sets the {@link ecm.model.ContentItem} object to render.
             */
            setItem: function (item, pageNumber) {
                this.inherited(arguments);
                this.item = item;
            },

            /**
             * Shows the content for the {@link ecm.model.ContentItem} object.
             *
             * @param callback
             *            An optional callback function the caller can provide to take additional
             *            action after the content has been rendered.
             */
            showItem: function (callback) {
                var methodName = "showItem";
                this.logEntry(methodName);

                /**
                 * Custom code to show the item content content. To check for a callback method you
                 * should use the dojo/_base/lang isFunction method. For example:
                 *
                 * if (lang.isFunction(callback)) {
                 * 		callback();
                 * }
                 */

                this.logExit(methodName);
            }
        });
    });
