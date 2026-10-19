# webtoolkit-icn-plugin: ICN Plugin for jadice web viewer

jadice® web viewer is a web-based multi-format viewer for several formats like PDF, TIFF, AFP, MO:DCA, PNG, JPEG and more. 
With this plugin jadice can be configured in IBM Content Navigator as viewer for documents instead of Daeja One Viewer.

## Features

Below is a small list with features of the jadice web viewer

| Feature | Available |
|---|---|
| Display documents of any Content Navigator repository | yes (upload mode) |
| P8 annotations: read and write via the viewer | yes (token mode) |
| No document transfer, load-balancer safe | yes (token mode) |
| No P8 credentials in Content Navigator | yes |

## Version support

This plugin requires Content Navigator 3.0.x; it was tested with 3.0.7 and jadice web viewer 2.0 (jadice web
toolkit 8.0).

## Installation

### Plugin Installation
1. Make sure the plugin JAR file (`target/webtoolkit-icn-plugin-<version>.jar`, see Development) is accessible
   from the Content Navigator server
2. Install the plugin via the admin-panel
3. Configure the plugin (see below): the HTTPS "jadice web viewer URL" and either the token secret (P8 annotations)
   or the Content Navigator server URL (display only).
4. Create a view-mapping for the new plugin in the admin-panel
5. Make sure the view-mapping is applied for the desired desktop

### jadice web viewer Installation

Deploy jadice web viewer as a standalone application, for example with
Spring Boot, and serve it over **HTTPS**. The "jadice web viewer URL" configured in the plugin is used by the browser, which embeds the viewer in an
iframe, and, in display-only mode, by the Content Navigator server for the upload.

Content Navigator ships a Content Security Policy with `frame-src 'self' https:`. An HTTP viewer URL on another
origin is therefore blocked by the browser; an HTTPS URL is allowed without changing the Content Navigator
configuration. A reverse proxy or a deployment on the Content Navigator application server is not required.

If the viewer needs its own session cookie inside the iframe and Content Navigator itself runs on HTTP or on another
site, set the cookie to `SameSite=None; Secure` (Spring Boot: `server.servlet.session.cookie.same-site: none`).

### Plugin configuration: two modes

| Setting | With P8 annotations | Display only |
|---|---|---|
| jadice web viewer URL | required, `https://` | required, `https://` |
| Load and save P8 annotations via jadice | on | off |
| Token secret (at least 32 characters) | required | not used |
| Token lifetime (minutes) | optional, default 480 | not used |
| Content Navigator server URL | not used | required |

**With P8 annotations (token mode).** The plugin transfers no document at all. For a P8 document it builds a
signed, time-limited token with the P8 object store and document id, the user's annotation edit right and the ICN
user id, and opens the viewer with `?u={"uri":"icn://<token>"}`. The viewer verifies the token with the shared
secret (`webtoolkit.icn.token.secret`), reads content and annotations directly from P8 with its technical user
(`webtoolkit.icn.p8.uri`, `.user`, `.password`, optional `.domain`) and writes annotations back. Any instance behind
a load balancer can serve the token. Viewer side: `webtoolkit-content-navigator-integration` plus IBM's `Jace.jar` on
the classpath. Documents from non-P8 repositories are shown without annotations via the upload path below; this
needs the Content Navigator server URL as well, otherwise the plugin falls back to the request's own URL.

Security note for token mode: before signing, the plugin fetches the document through Content Navigator in the
user's own security context (`PluginServiceCallbacks.getP8Document`), so a user can only obtain tokens for documents
they may read, and the ids in the token come from that document, not from the request. The edit right in the token
requires both ICN's `editAnnotations` privilege and the FileNet "Link/Annotate" right on the document. The token also
carries the ICN user id (`sub`) so that the viewer can log who opened and annotated a document; it is used for
logging only, never for access decisions. Keep the secret out of logs and the ICN configuration export. Building the
plugin needs the FileNet `Jace.jar` on the classpath (`provided`, see `pom.xml`); the Content Navigator runtime
supplies it.

**Display only (upload mode).** The plugin, running inside the Content Navigator server, downloads the document
from the configured Content Navigator server URL and posts it as `multipart/form-data` to
`<jadice web viewer URL>/upload` (endpoint of the `webtoolkit-addon-upload` module). The response is an upload id;
the plugin opens the viewer with `?u={"uri":"upload://<uploadId>"}`. No P8 ids and no credentials are transferred,
annotations are neither read nor written, and the ICN integration module is not needed on the viewer side. Upload
limits are those of the viewer (`spring.servlet.multipart.max-file-size`). The `/upload` endpoint is
unauthenticated; restrict it to the ICN server at network level. Upload mode is not cluster-safe: the upload and
the viewer request must reach the same instance.

### How the viewer is embedded

The plugin registers its own viewer widget (`JwtPluginDojo.JWTViewerPlugin`, a `DocViewer` subclass). When a
document is opened, the widget calls the plugin service `jwtViewerPluginService` with `format=json`, receives
the viewer URL and sets it as the source of an iframe it owns. The URL carries `connection=longpoll`, `theme=icn`
(the viewer's Content Navigator theme) and the document source as `u={"uri":...}`. Errors from the service
(invalid configuration, missing access, viewer not reachable) are shown inside the viewer area.

## Integration

### jadice web viewer integration

For P8 annotations the viewer needs the following module (it pulls in `webtoolkit-p8-integration` and
`webtoolkit-addon-upload`); display-only installations need `webtoolkit-addon-upload` only:

```xml
<dependency>
    <groupId>com.levigo.jadice.webtoolkit</groupId>
    <artifactId>webtoolkit-content-navigator-integration</artifactId>
</dependency>
```

The module is part of the jadice web toolkit (the server runtime of the viewer) and bound to the respective version. More information can be found in the README.md of this module.

## Development

- Put `j2ee.jar` and `navigatorAPI.jar` from your Content Navigator installation under `/lib` (see `lib/README.md`).
- Install IBM's `Jace.jar` into your local Maven repository as `p8-client:jace:5.5.4.0` (command in `lib/README.md`);
  the plugin compiles against it with `provided` scope and never bundles it.
- `mvn package` builds `target/webtoolkit-icn-plugin-<version>.jar` and runs the unit tests (token codec, access
  check, configuration validation). The token test vector is shared with `IcnTokenCodecTest` in the jadice web
  toolkit; keep both identical.

## License

[Apache License 2.0](https://github.com/levigo/webtoolkit-icn-plugin/blob/master/LICENSE)
