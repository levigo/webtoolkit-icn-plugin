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

## Installation

### plugin installation
1. Make sure the plugin JAR-file (e.g. webtoolkit-content-navigator-plugin-1.0.0.jar) is accessible from Content Navigator
2. install the plugin via the admin-panel
3. make sure you correctly define the "jadice web toolkit server" field in the plugin installation process (e.g. http://myicnhost/p8integration , see below)
4. create a view-mapping for the new plugin in the admin-panel
5. make sure the view-mapping is applied for the desired desktop


### jadice web toolkit installation 

1. deploy the webtoolkit-content-navigator-integration WAR-file to the same application server as the Content Navigator. this is mandatory as the Content Navigator enforces same-origin-policy
2. deploy the WAR-file with the context path "p8integration" so that it is reachable via e.g. http://myicnhost/p8integration (where Content Navigator is accessible via http://myicnhost/navigator )

## Development

- Get a j2ee.jar (e.g. from your Application server) and place it under /lib
- Get navigatorAPI.jar from Content Navigator and place it under /lib
- Run maven task e.g. `mvn install`

## License

[Apache License 2.0](https://github.com/levigo/webtoolkit-icn-plugin/blob/master/LICENSE)