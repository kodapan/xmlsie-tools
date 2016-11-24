package se.sambruk.xmlsie.orebro;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.poi.ss.examples.ToCSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sie.xml.AccountTypeTYPE;
import se.sie.xml.JournalInfoTYPE;
import se.sie.xml.ObjectFactory;
import se.sie.xml.SIE;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Örebro-style XLSX/CSV file to XMLSIE 1.0.
 *
 * @author kalle
 * @since 2016-06-12 21:18
 */
public class XLSX2XMLSIEConverter {

  private static Logger log = LoggerFactory.getLogger(XLSX2XMLSIEConverter.class);

  public Reader convertXLS2CSV(File xlsFile) throws Exception {
    File csvPath = File.createTempFile(xlsFile.getName(), ".converted");
    csvPath.delete();
    csvPath.mkdirs();

    ToCSV toCSV = new ToCSV();
    toCSV.convertExcelToCSV(xlsFile.getAbsolutePath(), csvPath.getAbsolutePath(), "\t");
    File[] files = csvPath.listFiles();
    if (files.length != 1) {
      throw new RuntimeException("Multiple files generated from XLS conversion?!");
    }
    return new InputStreamReader(new FileInputStream(files[0]), "UTF8");
  }


  public void convert(File inputFile, Writer output) throws Exception {

    if (inputFile.getName().endsWith(".csv")) {
      convertFromCSV(new InputStreamReader(new FileInputStream(inputFile), "UTF8"), output);

    } else if (inputFile.getName().endsWith(".xlsx")
        || inputFile.getName().endsWith(".xls")) {
      convertFromCSV(convertXLS2CSV(inputFile), output);

    } else {
      throw new RuntimeException("Don't know how to treat file. Please use .csv, .xls or .xlss: " + inputFile.getName());

    }

  }

  public void convertFromCSV(Reader reader, Writer output) throws Exception {


    Map<Integer, Konto> konton = new HashMap<>();
    Map<String, Faktura> fakturor = new HashMap<>();
    Map<String, Mottagare> mottagare = new HashMap<>();

    BufferedReader br = new BufferedReader(reader);
    String line = br.readLine(); // skip header

    while ((line = br.readLine()) != null) {

      String[] columns = line.split("\t");
      if (columns.length != 6) {
        throw new RuntimeException("Rows must contain exactly 6 rows!");
      }
      for (int i = 0; i < columns.length; i++) {
        columns[i] = columns[i].trim();
        if (columns[i].startsWith("\"") && columns[i].endsWith("\"")) {
          columns[i] = columns[i].substring(1, columns[i].length() - 1);
          columns[i] = columns[i].trim();
        }
      }

      Matcher kontoMatcher = kontoPattern.matcher(columns[4]);
      if (!kontoMatcher.matches()) {
        throw new RuntimeException(columns[4]);
      }

      int kontonummer = Integer.valueOf(kontoMatcher.group(1));
      String kontonamn = kontoMatcher.group(2) == null ? "" : kontoMatcher.group(2).trim();
      Konto konto = konton.get(kontonummer);
      if (konto == null) {
        konto = new Konto();
        konto.nummer = kontonummer;
        konto.namn = kontonamn;
        konton.put(kontonummer, konto);
      } else {
        if (!konto.namn.equals(kontonamn)) {
          throw new RuntimeException();
        }
      }

      Mottagare mottagaren = mottagare.get(columns[0]);
      if (mottagaren == null) {
        mottagaren = new Mottagare();
        mottagaren.namn = columns[0];
        mottagare.put(mottagaren.namn, mottagaren);
      }

      String fakturanummer = columns[3];
      Faktura faktura = fakturor.get(fakturanummer);
      if (faktura == null) {
        faktura = new Faktura();
        faktura.nummer = fakturanummer;
        faktura.mottagare = mottagaren;
        faktura.leverantörsnamn = columns[1];
        faktura.leverantörsorganisationsnummer = columns[2];
        fakturor.put(fakturanummer, faktura);
        mottagaren.fakturor.add(faktura);
      } else {
        System.currentTimeMillis();
      }
      try {
        BigDecimal amount = new BigDecimal(columns[5].replaceAll(",", ""));
        faktura.konteringar.add(new Kontering(konto, amount));
      } catch (NumberFormatException nfe) {
        System.currentTimeMillis();
      }


    }

    List<Faktura> orderedFakturor = new ArrayList<>(fakturor.values());
    Collections.sort(orderedFakturor, new Comparator<Faktura>() {
      @Override
      public int compare(Faktura o1, Faktura o2) {
        return Integer.compare(o2.konteringar.size(), o1.konteringar.size());
      }
    });

    System.currentTimeMillis();


    ObjectFactory objectFactory = new ObjectFactory();

    SIE sie = objectFactory.createSIE();

    sie.setCompany(objectFactory.createSIECompany());
    sie.getCompany().setOrganizationalnumber("212000-1967");
    sie.getCompany().setName("Örebro kommun");
    sie.getCompany().setAddressLine1("BOX 30000");
    sie.getCompany().setPostcode("701 35");
    sie.getCompany().setCity("Örebro");
    sie.getCompany().setCountryCode("SE");
    sie.getCompany().setHomepage("http://www.orebro.se/");


    sie.setCurrency("SEK");
//    sie.setSIEType();


    // accounting

    sie.setAccounting(objectFactory.createSIEAccounting());
    sie.getAccounting().setAccounts(objectFactory.createSIEAccountingAccounts());

    Set<Integer> unknownKontonummer = new HashSet<>();

    for (Konto konto : konton.values()) {
      SIE.Accounting.Accounts.Account account = objectFactory.createSIEAccountingAccountsAccount();
      account.setName(konto.namn);
      account.setId(BigInteger.valueOf(konto.nummer));

      int typ = Integer.valueOf(String.valueOf(konto.nummer).substring(0, 1));
      if (typ == 1) {
        account.setType(AccountTypeTYPE.ASSET);
      } else if (typ == 2) {
        account.setType(AccountTypeTYPE.LIABILITY);
      } else if (typ == 3) {
        account.setType(AccountTypeTYPE.INCOME);
      } else if (typ == 4) {
        account.setType(AccountTypeTYPE.COST);
      } else {
        if (unknownKontonummer.add(konto.nummer)) {
          log.warn("Treating unknwon account " + konto.nummer + " as type COST.");
        }
        account.setType(AccountTypeTYPE.COST);
      }

      sie.getAccounting().getAccounts().getAccount().add(account);

    }

    Collections.sort(sie.getAccounting().getAccounts().getAccount(), new Comparator<SIE.Accounting.Accounts.Account>() {
      @Override
      public int compare(SIE.Accounting.Accounts.Account o1, SIE.Accounting.Accounts.Account o2) {
        return String.valueOf(o1.getId()).compareTo(String.valueOf(o2.getId()));
      }
    });


    // accounts payable

    sie.setAccountsPayable(objectFactory.createSIEAccountsPayable());

    // accounts payable suppliers

    sie.getAccountsPayable().setSuppliers(objectFactory.createSIEAccountsPayableSuppliers());

    Map<String, SIE.AccountsPayable.Suppliers.Supplier> suppliers = new HashMap<>();
    for (Faktura faktura : fakturor.values()) {
      SIE.AccountsPayable.Suppliers.Supplier supplier = suppliers.get(faktura.leverantörsorganisationsnummer);
      if (supplier == null) {
        supplier = objectFactory.createSIEAccountsPayableSuppliersSupplier();
        supplier.setSupplierId(String.valueOf(suppliers.size()));
        supplier.setSupplierName(faktura.leverantörsnamn);
        supplier.setSupplierOrganizationalNumber(faktura.leverantörsorganisationsnummer);
        suppliers.put(faktura.leverantörsorganisationsnummer, supplier);
        sie.getAccountsPayable().getSuppliers().getSupplier().add(supplier);
      }
    }

    // accounts payable invoices

    sie.getAccountsPayable().setInvoices(objectFactory.createSIEAccountsPayableInvoices());

    for (Faktura faktura : fakturor.values()) {

      BigDecimal total = new BigDecimal("0");
      for (Kontering kontering : faktura.konteringar) {
        total = total.add(kontering.amount);
      }


      SIE.AccountsPayable.Invoices.Invoice invoice = objectFactory.createSIEAccountsPayableInvoicesInvoice();

      invoice.setInternalId(faktura.nummer);
      invoice.setGrossAmount(total);
      invoice.setSupplierId(suppliers.get(faktura.leverantörsorganisationsnummer).getSupplierId());

      sie.getAccountsPayable().getInvoices().getInvoice().add(invoice);

      faktura.invoice = invoice;

    }

    // transactions

    sie.getAccounting().setFinancialYears(objectFactory.createSIEAccountingFinancialYears());

    SIE.Accounting.FinancialYears.FinancialYear financialYear = objectFactory.createSIEAccountingFinancialYearsFinancialYear();

    financialYear.setStart(new XMLGregorianCalendarImpl(GregorianCalendar.from(ZonedDateTime.parse("2016-04-01T00:00:00+01:00[Europe/Stockholm]"))));
    financialYear.setEnd(new XMLGregorianCalendarImpl(GregorianCalendar.from(ZonedDateTime.parse("2016-05-01T00:00:00+01:00[Europe/Stockholm]"))));

    financialYear.setJournals(objectFactory.createSIEAccountingFinancialYearsFinancialYearJournals());

    for (Mottagare mottagaren : mottagare.values()) {

      SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal journal = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournal();
      journal.setName(mottagaren.namn);
      journal.setId(String.valueOf(financialYear.getJournals().getJournal().size()));

      for (Faktura faktura : mottagaren.fakturor) {

        SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry journalEntry = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournalJournalEntry();

        journalEntry.setId(String.valueOf(journal.getJournalEntry().size()));

        for (Kontering kontering : faktura.konteringar) {
          SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry.LedgerEntry ledgerEntry = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournalJournalEntryLedgerEntry();
          ledgerEntry.setAccountId(BigInteger.valueOf(kontering.konto.nummer));
          ledgerEntry.setAmount(kontering.amount);
          journalEntry.getLedgerEntry().add(ledgerEntry);
        }

        journal.getJournalEntry().add(journalEntry);

        // associate invoice with journal entry
        JournalInfoTYPE journalInfo = new JournalInfoTYPE();
        journalInfo.setFinancialYear(financialYear.getStart());
        journalInfo.setJournalId(journal.getId());
        journalInfo.setJournalEntryId(journalEntry.getId());
        faktura.invoice.setJournalInfo(journalInfo);

      }
      financialYear.getJournals().getJournal().add(journal);

    }

    sie.getAccounting().getFinancialYears().getFinancialYear().add(financialYear);


    JAXBContext jaxbContext = JAXBContext.newInstance(SIE.class);
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

    // output pretty printed
    jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    jaxbMarshaller.marshal(sie, output);

  }


  private static Pattern kontoPattern = Pattern.compile("(\\d+)( (.+))?");

  private class Kontering {
    private Konto konto;
    private BigDecimal amount;

    private Kontering(Konto konto, BigDecimal amount) {
      this.konto = konto;
      this.amount = amount;
    }

    @Override
    public String toString() {
      return "Kontering{" +
          "konto=" + konto +
          ", amount=" + amount +
          '}';
    }
  }

  private class Konto {

    private int nummer;
    private String namn;

    @Override
    public String toString() {
      return "Konto{" +
          "nummer=" + nummer +
          ", namn='" + namn + '\'' +
          '}';
    }
  }

  private class Mottagare {
    private String namn;
    private List<Faktura> fakturor = new ArrayList<>();
  }

  private class Faktura {

    private SIE.AccountsPayable.Invoices.Invoice invoice;

    private Mottagare mottagare;
    private String leverantörsnamn;
    private String leverantörsorganisationsnummer;

    private String nummer;
    private List<Kontering> konteringar = new ArrayList<>();


    @Override
    public String toString() {
      return "Faktura{" +
          "nummer=" + nummer +
          ", konteringar.size=" + konteringar.size() +
          ", konteringar=" + konteringar +
          '}';
    }
  }
}
