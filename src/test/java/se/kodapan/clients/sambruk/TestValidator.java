package se.kodapan.clients.sambruk;

import junit.framework.Assert;
import org.junit.Test;
import se.sambruk.xmlsie.Validator;

/**
 * @author kalle
 * @since 2016-06-19 17:14
 */
public class TestValidator {


  @Test
  public void testFailingNonSIE() throws Exception {

    StringBuilder xml = new StringBuilder()
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        .append("<FAIL>\n")
        .append("</FAIL>\n");

    Validator validator = new Validator();
    Assert.assertFalse(validator.validate(xml.toString()));

    System.currentTimeMillis();

  }

  @Test
  public void testFailingAlmostSIE() throws Exception {

    StringBuilder xml = new StringBuilder()
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        .append("<SIE>\n")
        .append(" <Company\n")
        .append("   name=\"Örebro kommun\" organizationalnumber=\"212000-1967\"\n")
        .append("   addressLine1=\"BOX 30000\" postcode=\"701 35\" city=\"Örebro\" countryCode=\"SE\"\n")
        .append("   homepage=\"http://www.orebro.se/\"\n")
        .append(" />\n")
        .append("</SIE>\n");


    Validator validator = new Validator();
    Assert.assertFalse(validator.validate(xml.toString()));

    System.currentTimeMillis();

  }

  @Test
  public void testFailingNotUniqueAccount() throws Exception {

    StringBuilder xml = new StringBuilder()
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        .append("<SIE>\n")
        .append(" <Company\n")
        .append("   name=\"Örebro kommun\" organizationalnumber=\"212000-1967\"\n")
        .append("   addressLine1=\"BOX 30000\" postcode=\"701 35\" city=\"Örebro\" countryCode=\"SE\"\n")
        .append("   homepage=\"http://www.orebro.se/\"\n")
        .append(" />\n")

        .append(" <Accounting>\n")
        .append("  <Accounts>\n")
        .append("   <Account>\n")
        .append("    <Id>0</Id>\n")
        .append("    <Name>Name 1</Name>\n")
        .append("    <Type>COST</Type>\n")
        .append("   </Account>\n")
        .append("   <Account>\n")
        .append("    <Id>0</Id>\n")
        .append("    <Name>Name 2</Name>\n")
        .append("    <Type>COST</Type>\n")
        .append("   </Account>\n")
        .append("  </Accounts>\n")
        .append(" </Accounting>\n")

        .append("</SIE>\n");


    Validator validator = new Validator();
    Assert.assertFalse(validator.validate(xml.toString()));

    System.currentTimeMillis();

  }

  @Test
  public void testMinimalWorking() throws Exception {

    StringBuilder xml = new StringBuilder()
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        .append("<SIE>\n")
        .append(" <Company\n")
        .append("   name=\"Örebro kommun\" organizationalnumber=\"212000-1967\"\n")
        .append("   addressLine1=\"BOX 30000\" postcode=\"701 35\" city=\"Örebro\" countryCode=\"SE\"\n")
        .append("   homepage=\"http://www.orebro.se/\"\n")
        .append(" />\n")
        .append(" <Currency>SEK</Currency>\n")
        .append(" <Accounting>\n")
        .append("  <Accounts>\n")
        .append("   <Account>\n")
        .append("    <Id>60150</Id>\n")
        .append("    <Name>Lokalhyror parkering garage</Name>\n")
        .append("    <Type>COST</Type>\n")
        .append("   </Account>\n")
        .append("  </Accounts>\n")
        .append("  <FinancialYears>\n")
        .append("   <FinancialYear start=\"2016-04-01+02:00\" end=\"2016-05-01+02:00\">\n")
        .append("    <Journals>\n")
        .append("     <Journal>\n")
        .append("      <Id>27</Id>\n")
        .append("      <Name>VVV Vård- och omsorgsnämnd väster</Name>\n")
        .append("      <JournalEntry>\n")
        .append("       <Id>322</Id>\n")
        .append("       <LedgerEntry>\n")
        .append("        <AccountId>60150</AccountId>\n")
        .append("        <Amount>57.0</Amount>\n")
        .append("       </LedgerEntry>\n")
        .append("      </JournalEntry>\n")
        .append("    </Journals>\n")
        .append("   </FinancialYear>\n")
        .append("  </FinancialYears>\n")
        .append(" </Accounting>\n")
        .append(" <AccountsPayable>\n")
        .append("  <Suppliers>\n")
        .append("   <Supplier>\n")
        .append("    <SupplierId>0</SupplierId>\n")
        .append("    <SupplierName>ÖREBROBOSTÄDER AB</SupplierName>\n")
        .append("    <SupplierOrganizationalNumber>556334-8449</SupplierOrganizationalNumber>\n")
        .append("   </Supplier>\n")
        .append("  </Suppliers>\n")
        .append("  <Invoices>\n")
        .append("   <Invoice>\n")
        .append("    <SupplierId>0</SupplierId>\n")
        .append("    <InternalId>835595</InternalId>\n")
        .append("    <GrossAmount>57.0</GrossAmount>\n")
        .append("    <JournalInfo>\n")
        .append("     <FinancialYear>2016-04-01+02:00</FinancialYear>\n")
        .append("     <JournalId>27</JournalId>\n")
        .append("     <JournalEntryId>322</JournalEntryId>\n")
        .append("    </JournalInfo>\n")
        .append("   </Invoice>\n")
        .append(" </AccountsPayable>\n")
        .append("</SIE>\n");

    Validator validator = new Validator();
    boolean success = validator.validate(xml.toString());

    System.currentTimeMillis();

  }

}
