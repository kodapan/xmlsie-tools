package se.sambruk.xmlsie.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;

/**
 * @author kalle
 * @since 2017-04-24 22:07
 */
public class TestConverter {

  @Test
  public void testAgresso() throws Exception {

    Configuration configuration = objectMapper.readValue("" +
            "{\n" +
            "    \"allowErrors\": false,\n" +
            "    \"currency\": \"SEK\",\n" +
            "    \"ignoreFirstRow\": true,\n" +
            "    \"ignoreLastRow\": true,\n" +
            "    \"columns\": [\n" +
            "      \"JOURNAL\",\n" +
            "      \"SUPPLIER_NAME\",\n" +
            "      \"SUPPLIER_ORGANIZATION_NUMBER\",\n" +
            "      \"INVOICE_INTERNAL_IDENTITY\",\n" +
            "      \"ACCOUNT_NUMBER\",\n" +
            "      \"ACCOUNT_NAME\",\n" +
            "      \"AMOUNT_DEBITED\"\n" +
            "    ],\n" +
            "    \"financialYear\": {\n" +
            "      \"start\": \"2017-01-01T00:00:00+01:00\",\n" +
            "      \"end\": \"2017-02-01T00:00:00+01:00\"\n" +
            "    },\n" +
            "    \"company\": {\n" +
            "      \"homepage\": \"https://tolvan.se\",\n" +
            "      \"organizationNumber\": \"121212-1212\",\n" +
            "      \"name\": \"Tolvan AB\",\n" +
            "      \"addressLine1\": \"Gatan 123\",\n" +
            "      \"addressLine2\": \"C/O Tol Van\",\n" +
            "      \"postalCode\": \"123 45\",\n" +
            "      \"city\": \"Staden\",\n" +
            "      \"countryCode\": \"SE\"\n" +
            "    }\n" +
            "}"
        , Configuration.class);


    Converter converter = new Converter(configuration);
    StringWriter xmlSie = new StringWriter(49152);
    converter.convert(new File("src/test/resources/Levstat test 201601 Agresso.csv"), xmlSie);

    System.currentTimeMillis();

  }


  private ObjectMapper objectMapper = objectMapperProvider();

  public ObjectMapper objectMapperProvider() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

}
