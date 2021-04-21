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
                        this.renderQuality.set('value', jsonConfig.renderQuality);
                        console.log("JWT Server Uri: " + jsonConfig.jwtServerURI);
                        console.log("Render-Quality " + jsonConfig.renderQuality);
                    } else {
                        // Try to guess the URL
                        var href = window.location.href;
                        var ind = href.indexOf("/navigator");
                        if(ind >= 0) {
                            var jwtServerURI = href.substr(0, ind) + "/p8integration";
                            var configJson = {
                                "jwtServerURI": jwtServerURI
                            };
                            this.configurationString = JSON.stringify(configJson);
                            this.jwtServerURI.set('value', jwtServerURI);
                        }
                    }
                },

                _onFieldChange: function () {
                    var configJson = {
                        "jwtServerURI": this.jwtServerURI.get('value'),
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
                    console.warn("Invalid JWT server uri: " + jwtServerURI);
                    return false;
                }
            });
    });
