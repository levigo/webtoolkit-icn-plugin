# webtoolkit-icn-plugin: ICN Plugin for jadice web toolkit

jadice® web toolkit is a web-based multi-format viewer for several formats like PDF, TIFF, AFP, MO:DCA, PNG, JPEG and more. With this plugin jadice can be configured in IBM Content Navigator as viewer for documents instead of Daeja One Viewer.

## Features

Below is a small list with features of the jadice web toolkit viewer

| Feature   | Available          |
| --------  | ------------------ |
| P8 Annotations | :heavy_check_mark: |
| Filenet IS Annotations | :heavy_check_mark: |
| Extensibility | :star: :star: :star: |
| configurable render quality | :heavy_check_mark: |
| viewer can be used outside ICN | :heavy_check_mark: |
| OCR layer support | :heavy_check_mark: |
| Javascript API for viewer | :heavy_check_mark: |

## Version support

This plugins requires Content Navigator version 3.0.x and was tested with version 3.0.7 and version 3.0.9

## Installation

### plugin installation
1. Make sure the plugin JAR-file (e.g. webtoolkit-content-navigator-plugin-1.0.0.jar) is accessible from Content Navigator
2. install the plugin via the admin-panel
3. make sure you correctly define the "jadice web toolkit URL" field in the plugin installation process (e.g. http://myicnhost/webtoolkit , see below). This url is used by the Content Navigator server to transfer the documents to the jadice web toolkit.
5. make sure you correctly define the "Content Navigator server URL" field in the plugin installation process (e.g. http://myicnhost/webtoolkit , see below). This url is used by the Content Navigator in the viewing window of the web-browser (client). As the Content Navigator has a strict same-origin-policy, this URL must use the same base-path as the Content Navigator itself. To achieve this, either deploy the jadice web toolkit WAR-file on the same application server as the Content Navigator or use a proxy. Chapter 9.8 of the "*Customizing and Extending
   IBM Content Navigator*" Redbook provides more information about this setup.
6. create a view-mapping for the new plugin in the admin-panel
7. make sure the view-mapping is applied for the desired desktop


## Integration

### jadice web toolkit content navigator standalone viewer

The standalone viewer is ready-to-use and can directly be used as a viewer for the Content Navigator. It is an optimized version of the **enterprise demo** that is part of the jadice web toolkit artifact.

1. deploy the webtoolkit-content-navigator-integration WAR-file to the same application server as the Content Navigator. This is mandatory as the Content Navigator enforces same-origin-policy
2. deploy the WAR-file with the context path "p8integration" so that it is reachable via e.g. http://myicnhost/webtoolkit (where Content Navigator is accessible via http://myicnhost/navigator )

### existing jadice web toolkit integration

If you already have a running jadice web toolkit installation, you can use the following module to extend the integration, so it can communicate with the webtoolkit-icn-plugin

```xml
<dependency>
    <groupId>com.levigo.jadice.webtoolkit</groupId>
    <artifactId>webtoolkit-content-navigator-integration</artifactId>
</dependency>
```

The module is part of the jadice web toolkit and bound to the respective version. More information can be found in the README.md of this module.

## Development

- Get a j2ee.jar (e.g. from your Application server) and place it under /lib
- Get navigatorAPI.jar from Content Navigator and place it under /lib
- Run maven task e.g. `mvn install`

## License

[Apache License 2.0](https://github.com/levigo/webtoolkit-icn-plugin/blob/master/LICENSE)