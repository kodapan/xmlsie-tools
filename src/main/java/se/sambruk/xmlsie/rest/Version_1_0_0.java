package se.sambruk.xmlsie.rest;

import org.apache.poi.util.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import se.sambruk.xmlsie.Validator;
import se.sambruk.xmlsie.orebro.Converter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;

/**
 * @author kalle
 * @since 2016-06-19 02:55
 */
@Path("1.0.0")
public class Version_1_0_0 {

  @Path("convert")
  @Produces(MediaType.TEXT_XML)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response convert(
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

          Converter converter = new Converter();
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
        .header("Content-Disposition", "attachment; filename=\"" + fileDetail.getFileName() + ".sie.xml\"")
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

}
