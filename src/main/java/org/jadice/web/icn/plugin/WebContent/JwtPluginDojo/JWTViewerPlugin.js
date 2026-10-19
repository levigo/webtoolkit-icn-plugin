define([
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/dom-style",
        "ecm/model/Request",
        "ecm/widget/viewer/DocViewer",
        "dojo/text!./templates/JWTViewerPlugin.html",
        "ecm/LoggerMixin"
    ],
    function (declare, lang, domStyle, Request, DocViewer, template, LoggerMixin) {

        /**
         * Frameless viewer widget: asks the plugin service (format=json) for the jadice web viewer URL of the
         * current item and shows it in an iframe this widget owns. The widget never reads from the iframe, so
         * jadice web viewer may run on another origin of the same site (e.g. jwv.example.com next to
         * icn.example.com); a reverse proxy is not needed for the frame itself.
         */
        return declare("JwtPluginDojo.JWTViewerPlugin", [
            DocViewer,
            LoggerMixin
        ], {
            templateString: template,

            _loadedItemId: null,

            setItem: function (item, pageNumber) {
                this.inherited(arguments);
                this._item = item;
            },

            showItem: function (callback) {
                var methodName = "showItem";
                this.logEntry(methodName);

                if (!this._item) {
                    this._showMessage("No item to show.");
                    this.logExit(methodName);
                    return;
                }
                if (this._loadedItemId === this._item.id) {
                    // same item shown again (tab re-activated): keep the running viewer
                    if (lang.isFunction(callback)) {
                        callback();
                    }
                    this.logExit(methodName);
                    return;
                }

                this._loadedItemId = this._item.id;
                this._hideMessage();
                this.setIsLoading(true);

                var item = this._item;
                var repository = item.repository;
                var docUrlParams = {
                    docid: item.id,
                    repositoryId: repository.id,
                    template_name: item.template,
                    vsId: item.vsId
                };
                Request.setSecurityToken(docUrlParams);
                // same URL Content Navigator's own viewers download the content from; the service reads
                // repositoryId and vsId from its query string and, in upload mode, downloads the content from it
                var docUrl = Request.getServiceRequestUrl("getDocument", repository.type, docUrlParams);
                // getServiceRequestUrl answers relative to the navigator root ("./jaxrs/..."); the service needs
                // an absolute path so it can resolve it against the configured Content Navigator server URL
                if (docUrl.indexOf("./") === 0 && ecm.model.desktop && lang.isFunction(ecm.model.desktop.getServicesUrl)) {
                    docUrl = ecm.model.desktop.getServicesUrl() + docUrl.substring(1);
                }

                var requestParams = {
                    format: "json",
                    docUrl: docUrl,
                    docId: item.id,
                    contentType: item.mimetype || item.contentType || "",
                    serverType: repository.type,
                    viewAnnotations: String(this._hasPrivilege(item, ["privViewAnnotations", "privViewAnnotation"])),
                    editAnnotations: String(this._hasPrivilege(item, ["privEditAnnotations", "privEditAnnotation"]))
                };

                Request.invokePluginService("JWTPlugin", "jwtViewerPluginService", {
                    requestParams: requestParams,
                    requestCompleteCallback: lang.hitch(this, function (data) {
                        this.setIsLoading(false);
                        if (data && data.url) {
                            this.logDebug(methodName, "opening jadice web viewer");
                            this.frameNode.src = data.url;
                            this.onDocumentLoaded();
                        } else {
                            this._loadedItemId = null;
                            this._showMessage((data && data.error) || "jadice web viewer could not be opened.");
                        }
                        if (lang.isFunction(callback)) {
                            callback();
                        }
                    }),
                    requestFailedCallback: lang.hitch(this, function (response, errorMessages) {
                        this.setIsLoading(false);
                        this._loadedItemId = null;
                        var text = (lang.isArray(errorMessages) && errorMessages.length > 0 && errorMessages[0].text)
                            || "jadice web viewer could not be opened.";
                        this._showMessage(text);
                        if (lang.isFunction(callback)) {
                            callback();
                        }
                    }),
                    backgroundRequest: true
                });

                this.logExit(methodName);
            },

            /**
             * Content Navigator exposes item privileges through hasPrivilege(name); the exact privilege names
             * differ between versions, so several are tried. The service verifies the edit right against P8 anyway.
             */
            _hasPrivilege: function (item, names) {
                if (!item || !lang.isFunction(item.hasPrivilege)) {
                    return false;
                }
                for (var i = 0; i < names.length; i++) {
                    try {
                        if (item.hasPrivilege(names[i])) {
                            return true;
                        }
                    } catch (e) {
                        // unknown privilege name in this ICN version: try the next one
                    }
                }
                return false;
            },

            _showMessage: function (text) {
                this.messageNode.innerHTML = "";
                this.messageNode.appendChild(document.createTextNode(text));
                domStyle.set(this.messageNode, "display", "block");
            },

            _hideMessage: function () {
                domStyle.set(this.messageNode, "display", "none");
            },

            destroy: function () {
                if (this.frameNode) {
                    this.frameNode.src = "about:blank";
                }
                this._loadedItemId = null;
                this.inherited(arguments);
            }
        });
    });
