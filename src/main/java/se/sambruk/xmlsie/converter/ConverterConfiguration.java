package se.sambruk.xmlsie.converter;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author kalle
 * @since 2017-03-01 07:45
 */
@Data
public class ConverterConfiguration {

  private boolean allowErrors = false;

  private String currency = "SEK";
  private Company company;
  private FinancialYear financialYear;

  private Column[] columns;

  @Data
  public static class Company {
    private String homepage;
    private String organizationalnumber;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String postcode;
    private String city;
    private String countryCode = "SE";
  }

  @Data
  public static class FinancialYear {
    private OffsetDateTime start;
    private OffsetDateTime end;
  }

  public enum AccountType {
    ASSET,
    LIABILITY,
    INCOME,
    COST
  }

  public enum ColumnStereotype {
    JOURNAL,
    SUPPLIER_NAME,
    SUPPLIER_ORGANIZATION_NUMBER,
    INVOICE_INTERNAL_IDENTITY,
    ACCOUNT_NUMBER,
    ACCOUNT_NAME,
    ACCOUNT_NUMBER_AND_NAME,
    AMOUNT_DEBITED,
  }

  @Data
  public static class Column {
    private ColumnStereotype stereotype;
  }

}
