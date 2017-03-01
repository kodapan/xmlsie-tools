package se.sambruk.xmlsie.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.Set;

/**
 * @author kalle
 * @since 2016-06-19 03:14
 */
@javax.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> resources = new java.util.HashSet<>();
    resources.add(Version_1_0_0.class);
    resources.add(Version_1_0_1.class);
    resources.add(Version_1_0_2.class);
    resources.add(MultiPartFeature.class);
    return resources;
  }

}
