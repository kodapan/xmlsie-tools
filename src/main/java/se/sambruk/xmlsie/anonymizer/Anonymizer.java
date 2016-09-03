package se.sambruk.xmlsie.anonymizer;

import se.sie.xml.SIE;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.Reader;

/**
 * @author kalle
 * @since 2016-09-02 21:07
 */
public abstract class Anonymizer {

  public SIE anonymize(Reader xml) throws Exception {
    Unmarshaller unmarshaller = JAXBContext.newInstance(SIE.class).createUnmarshaller();
    SIE sie = (SIE) unmarshaller.unmarshal(xml);
    anonymize(sie);
    return sie;
  }


  public abstract void anonymize(SIE sie) throws Exception;

}
