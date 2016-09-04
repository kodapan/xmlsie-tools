# xmlsie-tools
REST service for validating integrity of XMLSIE files as used by the Sambruk open data accounts payable project.

##### Dependencies
  - Java 8
  - Maven

##### Usage

Start the http server on localhost:8080:
```sh
export MAVEN_OPTS="-Xmx2g"
mvn clean install
mvn jetty:run
```

Point your browser at
 - <http://localhost:8080/validate.html> for validation of XMLSIE-files.
 - <http://localhost:8080/convert.html> for convertion of Örebro-style XLSX or CSV to XMLSIE.
 - <http://localhost:8080/anonymize.html> to anonymize sole trader suppliers of accounts payable in XMLSIE.

These webpages are HTML implementations of REST API's that both accept a single file as MULTIPART_FORM_DATA via HTTP POST:
 - <http://localhost:8080/api/1.0.0/validate> accepts XMLSIE.
 - <http://localhost:8080/api/1.0.0/convert> accepts XLSX and CSV.
 - <http://localhost:8080/api/1.0.0/anonymize> accepts XMLSIE.
