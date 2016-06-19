package se.sambruk.xmlsie.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * @author kalle
 * @since 2016-06-19 03:14
 */
@javax.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> resources = new java.util.HashSet<>();
    resources.add(Version_1_0_0.class);
    resources.add(MultiPartFeature.class);
    return resources;
  }
}
