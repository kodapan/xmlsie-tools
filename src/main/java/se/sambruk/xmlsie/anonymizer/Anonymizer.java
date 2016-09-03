package se.sambruk.xmlsie.anonymizer;

import se.sie.xml.SIE;

/**
 * @author kalle
 * @since 2016-09-02 21:07
 */
public interface Anonymizer {

  public void anonymize(SIE sie) throws Exception;

}
