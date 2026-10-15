define(["dojo/_base/declare", "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/form/CheckBox",
        "ecm/widget/admin/PluginConfigurationPane",
        "dojo/text!./templates/ConfigurationPane.html"],
    function (declare, _TemplatedMixin, _WidgetsInTemplateMixin, CheckBox,
              PluginConfigurationPane, template) {

        return declare("JwtPluginDojo.ConfigurationPane", [
                PluginConfigurationPane, _TemplatedMixin, _WidgetsInTemplateMixin],
            {

                templateString: template,
                widgetsInTemplate: true,

                load: function (callback) {
                    if (this.configurationString) {
                        var jsonConfig = JSON.parse(this.configurationString);

                        this.jwtServerURI.set("value", jsonConfig.jwtServerURI);
                        this.renderQuality.set("value", jsonConfig.renderQuality);
                        this.icnServerURI.set('value', jsonConfig.icnServerURI);
                        this.p8AnnotationsEnabled.set("checked", jsonConfig.p8AnnotationsEnabled === true);
                        console.log("JWT Server URL: " + jsonConfig.jwtServerURI);
                        console.log("Render-Quality " + jsonConfig.renderQuality);
                    } else {
                        // Try to guess the URL
                        var href = window.location.href;
                        var ind = href.indexOf("/navigator");
                        if (ind >= 0) {
                            var jwtServerURI = href.substr(0, ind) + "/webtoolkit";
                            var configJson = {
                                "jwtServerURI": jwtServerURI
                            };
                            this.configurationString = JSON.stringify(configJson);
                            this.jwtServerURI.set("value", jwtServerURI);
                        }
                    }
                },

                _onFieldChange: function () {
                    var configJson = {
                        "jwtServerURI": this.jwtServerURI.get("value"),
                        "renderQuality": this.renderQuality.get("value"),
                        "icnServerURI": this.icnServerURI.get("value"),
                        "p8AnnotationsEnabled": this.p8AnnotationsEnabled.get("checked") === true
                    };
                    this.configurationString = JSON.stringify(configJson);
                    this.onSaveNeeded(true);
                },

                validate: function () {
                    var jwtServerURI = this.jwtServerURI.get("value");
                    if (!!jwtServerURI && jwtServerURI.length > 0) {
                        return true;
                    }
                    console.warn("Invalid JWT server uri: " + jwtServerURI);
                    return false;
                }
            });
    });
