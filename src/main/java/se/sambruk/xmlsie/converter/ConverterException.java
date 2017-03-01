package se.sambruk.xmlsie.converter;

/**
 * @author kalle
 * @since 2017-03-01 21:26
 */
public class ConverterException extends Exception {

  public ConverterException(String message) {
    super(message);
  }

  public ConverterException(String message, Exception rootCause) {
    super(message, rootCause);
  }

  public ConverterException(int lineNumber, String line, Exception rootCause) {
    this("Error on line " + lineNumber + ": " + rootCause.getMessage() + "\n" + line, rootCause);
  }
  
}
