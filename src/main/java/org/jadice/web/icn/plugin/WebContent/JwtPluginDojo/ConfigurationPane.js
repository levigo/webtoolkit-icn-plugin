define(["dojo/_base/declare", "dojo/dom-style", "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/form/CheckBox",
        "ecm/widget/admin/PluginConfigurationPane",
        "dojo/text!./templates/ConfigurationPane.html"],
    function (declare, domStyle, _TemplatedMixin, _WidgetsInTemplateMixin, CheckBox,
              PluginConfigurationPane, template) {

        return declare("JwtPluginDojo.ConfigurationPane", [
                PluginConfigurationPane, _TemplatedMixin, _WidgetsInTemplateMixin],
            {

                templateString: template,
                widgetsInTemplate: true,

                load: function (callback) {
                    if (this.configurationString) {
                        var jsonConfig = JSON.parse(this.configurationString);
                        this.viewerUrl.set("value", jsonConfig.viewerUrl || "");
                        this.icnServerURI.set("value", jsonConfig.icnServerURI || "");
                        this.p8AnnotationsEnabled.set("checked", jsonConfig.p8AnnotationsEnabled === true);
                        this.tokenSecret.set("value", jsonConfig.tokenSecret || "");
                        this.tokenTtlMinutes.set("value", jsonConfig.tokenTtlMinutes || "480");
                    } else {
                        this.tokenTtlMinutes.set("value", "480");
                    }
                    this._showModeFields();
                },

                // Enabled: token mode (secret + lifetime). Disabled: upload mode (Content Navigator server URL).
                _showModeFields: function () {
                    var tokenMode = this.p8AnnotationsEnabled.get("checked") === true;
                    domStyle.set(this.tokenSecretRow, "display", tokenMode ? "" : "none");
                    domStyle.set(this.tokenTtlRow, "display", tokenMode ? "" : "none");
                    domStyle.set(this.icnServerRow, "display", tokenMode ? "none" : "");
                    this.tokenSecret.set("required", tokenMode);
                    this.icnServerURI.set("required", !tokenMode);
                },

                _onModeChange: function () {
                    this._showModeFields();
                    this._onFieldChange();
                },

                _onFieldChange: function () {
                    var configJson = {
                        "viewerUrl": this.viewerUrl.get("value"),
                        "icnServerURI": this.icnServerURI.get("value"),
                        "p8AnnotationsEnabled": this.p8AnnotationsEnabled.get("checked") === true,
                        "tokenSecret": this.tokenSecret.get("value"),
                        "tokenTtlMinutes": this.tokenTtlMinutes.get("value")
                    };
                    this.configurationString = JSON.stringify(configJson);
                    this.onSaveNeeded(true);
                },

                // Mirrors PluginConfig.validate() on the server.
                validate: function () {
                    var viewerUrl = this.viewerUrl.get("value") || "";
                    if (!/^https:\/\/.+/i.test(viewerUrl)) {
                        console.warn("jadice web viewer URL must use https://: " + viewerUrl);
                        return false;
                    }
                    if (this.p8AnnotationsEnabled.get("checked") === true) {
                        var secret = this.tokenSecret.get("value") || "";
                        if (secret.length < 32) {
                            console.warn("Token secret must have at least 32 characters");
                            return false;
                        }
                    } else if (!(this.icnServerURI.get("value") || "").trim()) {
                        console.warn("Content Navigator server URL is required without P8 annotations");
                        return false;
                    }
                    return true;
                }
            });
    });
