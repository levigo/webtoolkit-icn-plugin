define(["dojo/_base/declare", "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "ecm/widget/admin/PluginConfigurationPane",
        "dojo/text!./templates/ConfigurationPane.html"],
    function (declare, _TemplatedMixin, _WidgetsInTemplateMixin,
              PluginConfigurationPane, template) {

        return declare("jWTPluginDojo.ConfigurationPane", [
                PluginConfigurationPane, _TemplatedMixin, _WidgetsInTemplateMixin],
            {

                templateString: template,
                widgetsInTemplate: true,

                load: function (callback) {
                    if (this.configurationString) {
                        var jsonConfig = JSON.parse(this.configurationString);

                        this.jwtServerURI.set('value', jsonConfig.jwtServerURI);
                        this.icnServerURI.set('value', jsonConfig.icnServerURI);
                        this.renderQuality.set('value', jsonConfig.renderQuality);
                        console.log("JWT Server URL: " + jsonConfig.jwtServerURI);
                        console.log("ICN Server URL: " + jsonConfig.icnServerURI);
                        console.log("Render-Quality " + jsonConfig.renderQuality);
                    } else {
                        // Try to guess the URL
                        var href = window.location.href;
                        var ind = href.indexOf("/navigator");
                        if (ind >= 0) {
                            var jwtServerURI = href.substr(0, ind) + "/p8integration";
                            var icnServerURI = href.substr(0, ind);
                            var configJson = {
                                "jwtServerURI": jwtServerURI,
                                "icnServerURI": icnServerURI
                            };
                            this.configurationString = JSON.stringify(configJson);
                            this.jwtServerURI.set('value', jwtServerURI);
                            this.icnServerURI.set('value', icnServerURI);
                        }
                    }
                },

                _onFieldChange: function () {
                    var configJson = {
                        "jwtServerURI": this.jwtServerURI.get('value'),
                        "icnServerURI": this.icnServerURI.get('value'),
                        "renderQuality": this.renderQuality.get('value')
                    };
                    this.configurationString = JSON.stringify(configJson);
                    this.onSaveNeeded(true);
                },

                validate: function () {
                    var jwtServerURI = this.jwtServerURI.get('value');
                    if (!!jwtServerURI && jwtServerURI.length > 0) {
                        return true;
                    }
                    var icnServerURI = this.icnServerURI.get('value');
                    if (!!icnServerURI && icnServerURI.length > 0) {
                        return true;
                    }
                    console.warn("Invalid JWT server uri: " + jwtServerURI);
                    return false;
                }
            });
    });
