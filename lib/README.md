Jars that are not part of this repository:

- `j2ee.jar` and `navigatorAPI.jar` from your IBM Content Navigator installation go into this folder
  (`system` scope in `pom.xml`).
- `Jace.jar` (FileNet P8 client, needed for the access check in token mode) is resolved from Maven as
  `p8-client:jace:5.5.4.0` with `provided` scope. Install it from your FileNet installation once:
  `mvn install:install-file -Dfile=Jace.jar -DgroupId=p8-client -DartifactId=jace -Dversion=5.5.4.0 -Dpackaging=jar`.

None of these jars may be committed or redistributed.
