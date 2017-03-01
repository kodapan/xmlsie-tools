package se.sambruk.xmlsie.rest;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sambruk.xmlsie.SwedishOrganizationNumber;
import se.sambruk.xmlsie.converter.Converter;
import se.sambruk.xmlsie.converter.ConverterConfiguration;
import se.sambruk.xmlsie.converter.ConverterException;
import sun.nio.ch.IOUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * @author kalle
 * @since 2016-06-19 02:55
 */
@Path("1.0.2")
public class Version_1_0_2 {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Path("convert")
  @Produces(MediaType.TEXT_XML)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public javax.ws.rs.core.Response xlsx2xmlsie(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail,

      /* configuration */
      @FormParam("allowErrors") @DefaultValue("false") boolean allowErrors,

      @FormParam("columns") ConverterConfiguration.Column[] columns,

      @FormParam("currency") @DefaultValue("SEK") String currency,

      /* configuration.company */
      @FormParam("homepage") String homepage,
      @FormParam("organizationalnumber") String organizationalnumber,
      @FormParam("name") String name,
      @FormParam("addressLine1") String addressLine1,
      @FormParam("addressLine2") String addressLine2,
      @FormParam("postcode") String postcode,
      @FormParam("city") String city,
      @FormParam("countryCode") @DefaultValue("SE") String countryCode,

      /* configuration.financialYear */
      @FormParam("start") String start,
      @FormParam("end") String end

  ) throws Exception {

    if (columns == null || columns.length == 0) {
      throw new IllegalArgumentException("Missing value for parameter columns");
    }
    // todo assert required columns available

    if (countryCode == null || countryCode.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter countryCode");
    }
    countryCode = countryCode.trim();
    if (!countryCode.matches("^[A-Z]{2}$")) {
      throw new IllegalArgumentException("Parameter countryCode does not match pattern ^[A-Z]{2}$");
    }

    if (homepage == null) {
      throw new IllegalArgumentException("Missing value for parameter homepage");
    }
    homepage = homepage.trim();
    if (!homepage.toLowerCase().matches("^https?://")) {
      throw new IllegalArgumentException("Parameter homepage does not match pattern ^https?://");
    }

    if (city == null || city.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter city");
    }
    city = city.trim();

    if (postcode == null || postcode.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter postcode");
    }
    postcode = postcode.replaceAll("\\s", "");
    if (!postcode.matches("^\\d{5}$")) {
      throw new IllegalArgumentException("Parameter postcode does not match pattern ^\\d{5}$");
    }

    if (addressLine1 == null || addressLine1.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter addressLine1");
    }
    addressLine1 = addressLine1.trim();

    if (addressLine2 != null) {
      addressLine2 = addressLine2.trim();
      if (addressLine2.isEmpty()) {
        addressLine2 = null;
      }
    }

    if (organizationalnumber == null || organizationalnumber.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter organizationalnumber");
    }
    organizationalnumber = organizationalnumber.replaceAll("\\s", "");
    if (!organizationalnumber.matches("^\\d{6}-?\\d{4}$")) {
      throw new IllegalArgumentException("Parameter organizationalnumber does not match pattern ^\\d{6}-?\\d{4}$");
    }
    if (!SwedishOrganizationNumber.isSwedishOrganizationNumber(organizationalnumber)) {
      throw new IllegalArgumentException("Parameter organizationalnumber is not a valid swedish organization number");
    }

    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing value for parameter name");
    }
    name = name.trim();


    if (start == null) {
      throw new IllegalArgumentException("Missing value for parameter start");
    }
    if (end == null) {
      throw new IllegalArgumentException("Missing value for parameter end");
    }


    final ConverterConfiguration configuration = new ConverterConfiguration();
    configuration.setCurrency(currency);
    configuration.setAllowErrors(allowErrors);
    configuration.setColumns(columns);

    configuration.setFinancialYear(new ConverterConfiguration.FinancialYear());
    configuration.getFinancialYear().setStart(OffsetDateTime.parse(start));
    configuration.getFinancialYear().setEnd(OffsetDateTime.parse(end));

    configuration.setCompany(new ConverterConfiguration.Company());
    configuration.getCompany().setHomepage(homepage);
    configuration.getCompany().setOrganizationalnumber(organizationalnumber);
    configuration.getCompany().setName(name);
    configuration.getCompany().setAddressLine1(addressLine1);
    configuration.getCompany().setAddressLine2(addressLine2);
    configuration.getCompany().setPostcode(postcode);
    configuration.getCompany().setCity(city);
    configuration.getCompany().setCountryCode(countryCode);

    final Converter converter = new Converter(configuration);

    File directory = File.createTempFile(fileDetail.getName(), ".dir");
    try {
      if (!directory.delete()) {
        throw new IOException("Unable to remove temporary file " + directory.getAbsolutePath());
      }
      if (!directory.mkdirs()) {
        throw new IOException("Unable to create temporary directory " + directory.getAbsolutePath());
      }
      File inputFile = new File(directory, fileDetail.getName());
      final File xmlSieFile = new File(directory, fileDetail.getName() + ".sie.xml");
      {
        OutputStreamWriter xmlSieWriter = new OutputStreamWriter(new FileOutputStream(xmlSieFile), StandardCharsets.UTF_8);
        IOUtils.copy(uploadedInputStream, new FileOutputStream(inputFile));
        converter.convert(inputFile, xmlSieWriter);
        xmlSieWriter.close();
      }
      StreamingOutput stream = new StreamingOutput() {
        @Override
        public void write(OutputStream os)
            throws IOException, WebApplicationException {

          if (converter.getErrors() != null && !converter.getErrors().isEmpty()) {
            // add errors as comments

            Writer out = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            BufferedReader xmlSieReader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlSieFile), StandardCharsets.UTF_8));
            String xmlHeader = xmlSieReader.readLine(); // XML header
            out.write(xmlHeader);
            out.write("\n<!--\nErrors while parsing:\n");
            for (ConverterException e : converter.getErrors())  {
              out.write(e.getMessage());
              out.write("\n");
            }
            out.write("-->\n");

            char[] buffer = new char[49152];
            int readChars;
            while ((readChars = xmlSieReader.read(buffer)) > 0) {
              out.write(buffer, 0, readChars);
            }

            out.flush();

          } else {
            IOUtils.copy(new FileInputStream(xmlSieFile), os);
          }


        }
      };

      Response.ResponseBuilder responseBuilder;
      if (converter.getErrors().isEmpty()) {
        responseBuilder = Response.status(Response.Status.OK);
      } else {
        responseBuilder = Response.status(Response.Status.BAD_REQUEST);
      }
      responseBuilder.entity(stream);
      responseBuilder.header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileDetail.getFileName(), StandardCharsets.UTF_8.name()) + ".sie.xml\"");
      return responseBuilder.build();

    } finally {
      FileUtils.deleteDirectory(directory);
    }


  }

  @Data
  public static class Request {
    private byte[] contentBase64;
    private String contentType;
    private ConverterConfiguration configuration;
  }

}
