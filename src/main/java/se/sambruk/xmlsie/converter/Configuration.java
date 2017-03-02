package se.sambruk.xmlsie.converter;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author kalle
 * @since 2017-03-01 07:45
 */
@Data
public class Configuration {

  private boolean allowErrors = false;

  private String currency = "SEK";
  private Company company;
  private FinancialYear financialYear;

  private List<ColumnStereotype> columns;

  @Data
  public static class Company {
    private String homepage;
    private String organizationNumber;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String postalCode;
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
    AMOUNT_DEBITED;

  }
}