package se.sambruk.xmlsie.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sambruk.xmlsie.SwedishOrganizationNumber;
import se.sambruk.xmlsie.converter.Converter;
import se.sambruk.xmlsie.converter.Configuration;
import se.sambruk.xmlsie.converter.ConverterException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author kalle
 * @since 2016-06-19 02:55
 */
@Path("1.0.2")
public class Version_1_0_2 {

  private Logger log = LoggerFactory.getLogger(getClass());

  private ObjectMapper objectMapper = objectMapperProvider();

  public ObjectMapper objectMapperProvider() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  @Path("convert")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_XML)
  @POST
  public javax.ws.rs.core.Response convert(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("configuration") String configurationJson
  ) throws Exception {

    Configuration configuration;

    try {
      configuration = objectMapper.readValue(configurationJson, Configuration.class);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }

    if (configuration == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration").type(MediaType.TEXT_PLAIN).build();
    }
    if (configuration.getFinancialYear() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.financialYear").type(MediaType.TEXT_PLAIN).build();
    }
    if (configuration.getColumns() == null
        || configuration.getColumns().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.columns").type(MediaType.TEXT_PLAIN).build();
    }
    // todo assert required columns available

    if (configuration.getCompany() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company").type(MediaType.TEXT_PLAIN).build();
    }
    if (configuration.getCompany().getCountryCode() == null
        || configuration.getCompany().getCountryCode().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.countryCode").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setCountryCode(configuration.getCompany().getCountryCode().trim());
    if (!configuration.getCompany().getCountryCode().matches("^[A-Z]{2}$")) {
      return Response.status(Response.Status.BAD_REQUEST).entity("configuration.company.countryCode does not match pattern ^[A-Z]{2}$").type(MediaType.TEXT_PLAIN).build();
    }

    if (configuration.getCompany().getHomepage() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.homepage").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setHomepage(configuration.getCompany().getHomepage().trim());
    if (!configuration.getCompany().getHomepage().toLowerCase().matches("^https?://.+")) {
      return Response.status(Response.Status.BAD_REQUEST).entity("configuration.company.homepage does not match pattern ^https?://.+").type(MediaType.TEXT_PLAIN).build();
    }

    if (configuration.getCompany().getCity() == null
        || configuration.getCompany().getCity().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.city").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setCity(configuration.getCompany().getCity().trim());

    if (configuration.getCompany().getPostalCode() == null
        || configuration.getCompany().getPostalCode().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.postalCode").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setPostalCode(configuration.getCompany().getPostalCode().replaceAll("\\s", ""));
    if (!configuration.getCompany().getPostalCode().matches("^\\d{5}$")) {
      return Response.status(Response.Status.BAD_REQUEST).entity("configuration.company.postalCode does not match pattern ^\\d{5}$").type(MediaType.TEXT_PLAIN).build();
    }

    if (configuration.getCompany().getAddressLine1() == null
        || configuration.getCompany().getAddressLine1().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.addressLine1").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setAddressLine1(configuration.getCompany().getAddressLine1().trim());

    if (configuration.getCompany().getAddressLine2() == null
        || configuration.getCompany().getAddressLine2().trim().isEmpty()) {
      configuration.getCompany().setAddressLine2(null);
    } else {
      configuration.getCompany().setAddressLine2(configuration.getCompany().getAddressLine2().trim());
    }


    if (configuration.getCompany().getOrganizationNumber() == null
        || configuration.getCompany().getOrganizationNumber().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.organizationNumber").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setOrganizationNumber(configuration.getCompany().getOrganizationNumber().replaceAll("\\s", ""));
    if (!configuration.getCompany().getOrganizationNumber().matches("^\\d{6}-?\\d{4}$")) {
      return Response.status(Response.Status.BAD_REQUEST).entity("configuration.company.organizationNumber does not match pattern ^\\d{6}-?\\d{4}$").type(MediaType.TEXT_PLAIN).build();
    }
    if (!SwedishOrganizationNumber.isSwedishOrganizationNumber(configuration.getCompany().getOrganizationNumber())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("configuration.company.organizationNumber is not a valid swedish organization number").type(MediaType.TEXT_PLAIN).build();
    }

    if (configuration.getCompany().getName() == null
        || configuration.getCompany().getName().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.company.name").type(MediaType.TEXT_PLAIN).build();
    }
    configuration.getCompany().setName(configuration.getCompany().getName().trim());


    if (configuration.getFinancialYear().getStart() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.financialYear.start").type(MediaType.TEXT_PLAIN).build();
    }
    if (configuration.getFinancialYear().getEnd() == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing configuration.financialYear.end").type(MediaType.TEXT_PLAIN).build();
    }



    final Converter converter = new Converter(configuration);



    File directory = File.createTempFile(fileDetail.getName(), ".dir");
    try {
      if (!directory.delete()) {
        throw new IOException("Unable to remove temporary file " + directory.getAbsolutePath());
      }
      if (!directory.mkdirs()) {
        throw new IOException("Unable to create temporary directory " + directory.getAbsolutePath());
      }
      File inputFile = new File(directory, fileDetail.getFileName());
      final File xmlSieFile = new File(directory, fileDetail.getFileName() + ".sie.xml");
      {
        OutputStreamWriter xmlSieWriter = new OutputStreamWriter(new FileOutputStream(xmlSieFile), StandardCharsets.UTF_8);
        IOUtils.copy(uploadedInputStream, new FileOutputStream(inputFile));
        try {
          converter.convert(inputFile, xmlSieWriter);
        } catch (Exception e) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
        xmlSieWriter.close();
      }

      // todo create output before deleting file
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
            for (ConverterException e : converter.getErrors()) {
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
      FileUtils.deleteDirectory(directory);  // todo cause exception
    }


  }
  
}
