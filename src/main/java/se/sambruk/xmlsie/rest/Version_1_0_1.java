package se.sambruk.xmlsie.rest;

import org.apache.poi.util.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import se.sambruk.xmlsie.Validator;
import se.sambruk.xmlsie.anonymizer.Anonymizer;
import se.sambruk.xmlsie.anonymizer.SingleSoleTraderAnonymizer;
import se.sie.xml.SIE;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URLEncoder;

/**
 * @author kalle
 * @since 2016-06-19 02:55
 */
@Path("1.0.1")
public class Version_1_0_1 {

  @Path("xlsx2xmlsie/goteborg")
  @Produces(MediaType.TEXT_XML)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response xlsx2xmlsie(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail
  ) throws Exception {

    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os)
          throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(os, "UTF8");

        File file = File.createTempFile(fileDetail.getName(), ".tmp");
        try {
          IOUtils.copy(uploadedInputStream, new FileOutputStream(file));

          se.sambruk.xmlsie.goteborg.XLSX2XMLSIEConverter converter = new se.sambruk.xmlsie.goteborg.XLSX2XMLSIEConverter();
          try {
            converter.convertFromCSV(converter.convertXLS2CSV(file), writer);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

        } finally {
          file.delete();
        }


        writer.flush();
      }
    };
    return Response.ok(stream)
        .header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileDetail.getFileName(), "UTF8") + ".sie.xml\"")
        .build();


  }

  @Path("xmlsie2csv/goteborg")
  @Produces(MediaType.TEXT_PLAIN)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response xmlsie2csv_goteborg(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail
  ) throws Exception {

    se.sambruk.xmlsie.goteborg.XMLSIE2XLSXConverter converter = new se.sambruk.xmlsie.goteborg.XMLSIE2XLSXConverter();
    final StringWriter csv = new StringWriter();
    converter.convert(new InputStreamReader(uploadedInputStream, "UTF8"), csv);

    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os)
          throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(os, "UTF8");
        writer.write(csv.toString());
        writer.flush();
      }
    };

    String responseFileName = fileDetail.getFileName();
    if (responseFileName.toLowerCase().endsWith(".sie.xml")) {
      responseFileName = responseFileName.substring(0, responseFileName.length() - ".sie.xml".length());
    }
    responseFileName += ".csv";

    return Response.ok(stream)
        .header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(responseFileName, "UTF8") + "\"")
        .build();


  }

  //@Path("xlsx2xmlsie/orebro")
  @Path("xlsx2xmlsie")
  @Produces(MediaType.TEXT_XML)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response xlsx2xmlsie_orebro(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail
  ) throws Exception {

    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os)
          throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(os, "UTF8");

        File file = File.createTempFile(fileDetail.getName(), ".tmp");
        try {
          IOUtils.copy(uploadedInputStream, new FileOutputStream(file));

          se.sambruk.xmlsie.orebro.XLSX2XMLSIEConverter converter = new se.sambruk.xmlsie.orebro.XLSX2XMLSIEConverter();
          try {
            converter.convertFromCSV(converter.convertXLS2CSV(file), writer);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

        } finally {
          file.delete();
        }


        writer.flush();
      }
    };
    return Response.ok(stream)
        .header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileDetail.getFileName(), "UTF8") + ".sie.xml\"")
        .build();


  }

  //@Path("xmlsie2csv/orebro")
  @Path("xmlsie2csv")
  @Produces(MediaType.TEXT_PLAIN)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response xmlsie2csv_orebro(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @FormDataParam("file") final FormDataContentDisposition fileDetail
  ) throws Exception {

    se.sambruk.xmlsie.orebro.XMLSIE2XLSXConverter converter = new se.sambruk.xmlsie.orebro.XMLSIE2XLSXConverter();
    final StringWriter csv = new StringWriter();
    converter.convert(new InputStreamReader(uploadedInputStream, "UTF8"), csv);

    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os)
          throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(os, "UTF8");
        writer.write(csv.toString());
        writer.flush();
      }
    };

    String responseFileName = fileDetail.getFileName();
    if (responseFileName.toLowerCase().endsWith(".sie.xml")) {
      responseFileName = responseFileName.substring(0, responseFileName.length() - ".sie.xml".length());
    }
    responseFileName += ".csv";

    return Response.ok(stream)
        .header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(responseFileName, "UTF8") + "\"")
        .build();


  }

  @Path("validate")
  @Produces(MediaType.APPLICATION_JSON)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public String validate(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail
  ) throws Exception {

    JSONObject response = new JSONObject();

    Validator validator = new Validator();
    response.put("success", validator.validate(new InputStreamReader(uploadedInputStream, "UTF8")));
    if (!validator.getValidationErrors().isEmpty()) {
      JSONArray errors = new JSONArray();
      response.put("errors", errors);
      for (String validationError : validator.getValidationErrors()) {
        errors.put(validationError);
      }
    }
    if (!validator.getValidationWarnings().isEmpty()) {
      JSONArray warnings = new JSONArray();
      response.put("warnings", warnings);
      for (String validationWarning : validator.getValidationWarnings()) {
        warnings.put(validationWarning);
      }
    }
    return response.toString();

  }

  @Path("anonymize")
  @Produces(MediaType.TEXT_XML)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response anonymize(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail
  ) throws Exception {

    Anonymizer anonymizer = new SingleSoleTraderAnonymizer();
    SIE sie = anonymizer.anonymize(new InputStreamReader(uploadedInputStream, "UTF8"));

    JAXBContext jaxbContext = JAXBContext.newInstance(SIE.class);
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

    // output pretty printed
    jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    final StringWriter output = new StringWriter();
    jaxbMarshaller.marshal(sie, output);

    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os)
          throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(os, "UTF8");
        writer.write(output.toString());
        writer.flush();
      }
    };

    String responseFileName = fileDetail.getFileName();
    if (responseFileName.toLowerCase().endsWith(".sie.xml")) {
      responseFileName = responseFileName.substring(0, responseFileName.length() - ".sie.xml".length());
    }
    responseFileName += ".anonymized.sie.xml";

    return Response.ok(stream)
        .header("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(responseFileName, "UTF8") + "\"")
        .build();

  }

}
