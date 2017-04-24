package se.sambruk.xmlsie.converter;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.poi.ss.examples.ToCSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sambruk.xmlsie.Validator;
import se.sie.xml.AccountTypeTYPE;
import se.sie.xml.JournalInfoTYPE;
import se.sie.xml.ObjectFactory;
import se.sie.xml.SIE;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kalle
 * @since 2017-03-01 18:09
 */
public class Converter {


  private Logger log = LoggerFactory.getLogger(getClass());

  private Configuration configuration;

  private List<ConverterException> errors = new ArrayList<>();

  public Converter(Configuration configuration) {
    this.configuration = configuration;
  }

  private Reader convertXls2Csv(File xlsFile) throws Exception {
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

    String lowerCasedFileName = inputFile.getName().toLowerCase();

    if (lowerCasedFileName.endsWith(".csv")) {
      convertFromCsv(new InputStreamReader(new FileInputStream(inputFile), "UTF8"), output);

    } else if (lowerCasedFileName.endsWith(".xlsx")
        || inputFile.getName().toLowerCase().endsWith(".xls")) {
      convertFromCsv(convertXls2Csv(inputFile), output);

    } else {
      throw new RuntimeException("Don't know how to treat file. Please use .csv, .xls or .xlsx: " + inputFile.getName());

    }

  }

  private static Pattern accountNumberAndNamePattern = Pattern.compile("(\\d+)( (.+))?");


  public void convertFromCsv(Reader reader, Writer output) throws Exception {

    log.info("Reading rows...");

    BufferedReader br = new BufferedReader(reader);
    List<String> lines = new ArrayList<>(49152);
    {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) {
          lines.add(line);
        }
      }

      if (configuration.isIgnoreFirstRow()) {
        lines.remove(0);
      }
      if (configuration.isIgnoreLastRow()) {
        lines.remove(lines.size() - 1);
      }
    }

    log.info("Parsing CSV...");

    Map<Integer, FactoryAccount> factoryAccounts = new HashMap<>();
    Map<String, FactoryInvoice> factoryInvoices = new HashMap<>();
    Map<String, FactoryJournal> factorJournals = new HashMap<>();

    for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
      String line = lines.get(lineNumber);
      try {
        String[] columns = line.split("\t");
        if (columns.length != configuration.getColumns().size()) {
          throw new ConverterException(lineNumber, line, "Row must contain exactly " + configuration.getColumns().size() + " columns!");
        }
        for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
          columns[columnIndex] = columns[columnIndex].trim();
          if (columns[columnIndex].startsWith("\"") && columns[columnIndex].endsWith("\"")) {
            columns[columnIndex] = columns[columnIndex].substring(1, columns[columnIndex].length() - 1);
          }
          columns[columnIndex] = columns[columnIndex].trim();
        }

        Integer accountNumber = null;
        String accountName = null;
        String journalName = null;
        String invoiceNumber = null;
        String supplierName = null;
        String supplierOrganizationNumber = null;
        BigDecimal amountDebited = null;

        for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {

          if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.ACCOUNT_NUMBER) {
            accountNumber = Integer.valueOf(columns[columnIndex]);

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.ACCOUNT_NAME) {
            accountName = columns[columnIndex] == null ? "" : columns[columnIndex].trim();

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.ACCOUNT_NUMBER_AND_NAME) {
            Matcher matcher = accountNumberAndNamePattern.matcher(columns[columnIndex]);
            if (!matcher.matches()) {
              throw new ConverterException("Account name and number column does not match pattern " + accountNumberAndNamePattern.pattern());
            }

            accountNumber = Integer.valueOf(matcher.group(1));
            accountName = matcher.group(2) == null ? "" : matcher.group(2).trim();

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.JOURNAL) {
            journalName = columns[columnIndex];

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.INVOICE_INTERNAL_IDENTITY) {
            invoiceNumber = columns[columnIndex];

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.SUPPLIER_ORGANIZATION_NUMBER) {
            supplierOrganizationNumber = columns[columnIndex];

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.SUPPLIER_NAME) {
            supplierName = columns[columnIndex];

          } else if (configuration.getColumns().get(columnIndex) == Configuration.ColumnStereotype.AMOUNT_DEBITED) {
            amountDebited = new BigDecimal(columns[columnIndex].replaceAll(",", ""));

          } else {
            throw new ConverterException("Unsupported column stereotype " + configuration.getColumns().get(columnIndex));
          }
        }

        if (accountNumber == null) {
          throw new ConverterException("Missing account number");
        }
        if (accountName == null) {
          throw new ConverterException("Missing account name");
        }
        if (journalName == null) {
          throw new ConverterException("Missing journal name");
        }
        if (supplierName == null) {
          throw new ConverterException("Missing supplier name");
        }
        if (supplierOrganizationNumber == null) {
          throw new ConverterException("Missing supplier organization number");
        }
        if (amountDebited == null) {
          throw new ConverterException("Missing amount debited");
        }

        FactoryAccount factoryAccount = factoryAccounts.get(accountNumber);
        if (factoryAccount == null) {
          factoryAccount = new FactoryAccount();
          factoryAccount.setNumber(accountNumber);
          factoryAccount.setName(accountName);
          factoryAccounts.put(accountNumber, factoryAccount);
        } else {
          if (!factoryAccount.getName().equals(accountName)) {
            throw new ConverterException("Account name " + accountName + " does not match previously seen account name " + factoryAccount.getName());
          }
        }

        FactoryJournal factoryJournal = factorJournals.get(journalName);
        if (factoryJournal == null) {
          factoryJournal = new FactoryJournal();
          factoryJournal.setName(journalName);
          factorJournals.put(factoryJournal.getName(), factoryJournal);
        }

        FactoryInvoice factoryInvoice = factoryInvoices.get(invoiceNumber);
        if (factoryInvoice == null) {
          factoryInvoice = new FactoryInvoice();
          factoryInvoice.setNumber(invoiceNumber);
          factoryInvoice.setJournal(factoryJournal);
          factoryInvoice.setSupplierName(supplierName);
          factoryInvoice.setSupplierOrganizationNumber(supplierOrganizationNumber);
          factoryInvoices.put(invoiceNumber, factoryInvoice);
          factoryJournal.getInvoices().add(factoryInvoice);
        }

        factoryInvoice.getPostings().add(new FactoryInvoiceAccountAmountPosting(factoryAccount, amountDebited));


      } catch (ConverterException ce) {
        if (!configuration.isAllowErrors()) {
          throw ce;
        }
        errors.add(ce);

      } catch (Exception e) {
        ConverterException ce = new ConverterException(lineNumber, line, e);
        log.error("Caught exception", ce);
        if (!configuration.isAllowErrors()) {
          throw ce;
        }
        errors.add(ce);

      }
    }

    List<FactoryInvoice> orderedFactoryInvoices = new ArrayList<>(factoryInvoices.values());
    orderedFactoryInvoices.sort((o1, o2) -> Integer.compare(o2.getPostings().size(), o1.getPostings().size()));


    ObjectFactory objectFactory = new ObjectFactory();

    SIE sie = objectFactory.createSIE();

    sie.setCompany(objectFactory.createSIECompany());
    sie.getCompany().setOrganizationalnumber(configuration.getCompany().getOrganizationNumber());
    sie.getCompany().setName(configuration.getCompany().getName());
    sie.getCompany().setAddressLine1(configuration.getCompany().getAddressLine1());
    sie.getCompany().setAddressLine2(configuration.getCompany().getAddressLine2());
    sie.getCompany().setPostcode(configuration.getCompany().getPostalCode());
    sie.getCompany().setCity(configuration.getCompany().getCity());
    sie.getCompany().setCountryCode(configuration.getCompany().getCountryCode());
    sie.getCompany().setHomepage(configuration.getCompany().getHomepage());


    sie.setCurrency(configuration.getCurrency());
//    sie.setSIEType();


    // accounting

    sie.setAccounting(objectFactory.createSIEAccounting());
    sie.getAccounting().setAccounts(objectFactory.createSIEAccountingAccounts());

    Set<Integer> unknownAccountNumbers = new HashSet<>();

    for (FactoryAccount factoryAccount : factoryAccounts.values()) {
      SIE.Accounting.Accounts.Account sieAccount = objectFactory.createSIEAccountingAccountsAccount();
      sieAccount.setName(factoryAccount.getName());
      sieAccount.setId(BigInteger.valueOf(factoryAccount.getNumber()));

      int accountNumberPrefix = Integer.valueOf(String.valueOf(factoryAccount.getNumber()).substring(0, 1));
      if (accountNumberPrefix == 1) {
        sieAccount.setType(AccountTypeTYPE.ASSET);
      } else if (accountNumberPrefix == 2) {
        sieAccount.setType(AccountTypeTYPE.LIABILITY);
      } else if (accountNumberPrefix == 3) {
        sieAccount.setType(AccountTypeTYPE.INCOME);
      } else if (accountNumberPrefix >= 4) {
        sieAccount.setType(AccountTypeTYPE.COST);
      } else {
        if (unknownAccountNumbers.add(factoryAccount.getNumber())) {
          log.warn("Treating unknown account " + factoryAccount.getNumber() + " as type COST.");
        }
        sieAccount.setType(AccountTypeTYPE.COST);
      }

      sie.getAccounting().getAccounts().getAccount().add(sieAccount);

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
    for (FactoryInvoice factoryInvoice : factoryInvoices.values()) {
      SIE.AccountsPayable.Suppliers.Supplier supplier = suppliers.get(factoryInvoice.getSupplierOrganizationNumber());
      if (supplier == null) {
        supplier = objectFactory.createSIEAccountsPayableSuppliersSupplier();
        supplier.setSupplierId(String.valueOf(suppliers.size()));
        supplier.setSupplierName(factoryInvoice.getSupplierName());
        supplier.setSupplierOrganizationalNumber(factoryInvoice.getSupplierOrganizationNumber());
        suppliers.put(factoryInvoice.getSupplierOrganizationNumber(), supplier);
        sie.getAccountsPayable().getSuppliers().getSupplier().add(supplier);
      }
    }

    // accounts payable invoices

    sie.getAccountsPayable().setInvoices(objectFactory.createSIEAccountsPayableInvoices());

    for (FactoryInvoice factoryInvoice : factoryInvoices.values()) {

      BigDecimal total = new BigDecimal("0");
      for (FactoryInvoiceAccountAmountPosting kontering : factoryInvoice.getPostings()) {
        total = total.add(kontering.getAmount());
      }


      SIE.AccountsPayable.Invoices.Invoice invoice = objectFactory.createSIEAccountsPayableInvoicesInvoice();

      invoice.setInternalId(factoryInvoice.getNumber());
      invoice.setGrossAmount(total);
      invoice.setSupplierId(suppliers.get(factoryInvoice.getSupplierOrganizationNumber()).getSupplierId());

      sie.getAccountsPayable().getInvoices().getInvoice().add(invoice);

      factoryInvoice.setInvoice(invoice);

    }

    // transactions

    sie.getAccounting().setFinancialYears(objectFactory.createSIEAccountingFinancialYears());

    SIE.Accounting.FinancialYears.FinancialYear financialYear = objectFactory.createSIEAccountingFinancialYearsFinancialYear();

    financialYear.setStart(new XMLGregorianCalendarImpl(GregorianCalendar.from(configuration.getFinancialYear().getStart().toZonedDateTime())));
    financialYear.setEnd(new XMLGregorianCalendarImpl(GregorianCalendar.from(configuration.getFinancialYear().getEnd().toZonedDateTime())));

    financialYear.setJournals(objectFactory.createSIEAccountingFinancialYearsFinancialYearJournals());

    for (FactoryJournal factoryJournal : factorJournals.values()) {

      SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal journal = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournal();
      journal.setName(factoryJournal.getName());
      journal.setId(String.valueOf(financialYear.getJournals().getJournal().size()));

      for (FactoryInvoice factoryInvoice : factoryJournal.getInvoices()) {

        SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry journalEntry = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournalJournalEntry();

        journalEntry.setId(String.valueOf(journal.getJournalEntry().size()));

        for (FactoryInvoiceAccountAmountPosting posting : factoryInvoice.getPostings()) {
          SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry.LedgerEntry ledgerEntry = objectFactory.createSIEAccountingFinancialYearsFinancialYearJournalsJournalJournalEntryLedgerEntry();
          ledgerEntry.setAccountId(BigInteger.valueOf(posting.getAccount().getNumber()));
          ledgerEntry.setAmount(posting.getAmount());
          journalEntry.getLedgerEntry().add(ledgerEntry);
        }

        journal.getJournalEntry().add(journalEntry);

        // associate invoice with journal entry
        JournalInfoTYPE journalInfo = new JournalInfoTYPE();
        journalInfo.setFinancialYear(financialYear.getStart());
        journalInfo.setJournalId(journal.getId());
        journalInfo.setJournalEntryId(journalEntry.getId());
        factoryInvoice.getInvoice().setJournalInfo(journalInfo);

      }
      financialYear.getJournals().getJournal().add(journal);

    }

    sie.getAccounting().getFinancialYears().getFinancialYear().add(financialYear);


    if (!new Validator().validate(sie)) {
      throw new Exception("Exception while validating XML SIE produced by internals. Please contact developer! https://github.com/kodapan/xmlsie-tools/issues/new");
    }

    output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

    if (getErrors() != null && !getErrors().isEmpty()) {
      output.write("<!--\nErrors occurred while converting from single table file:\n\n");
      for (ConverterException e : getErrors()) {
        output.write(e.getMessage());
        output.write("\n\n");
      }
      output.write("-->\n");
    }

    JAXBContext jaxbContext = JAXBContext.newInstance(SIE.class);
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

    // output pretty printed
    jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    jaxbMarshaller.marshal(sie, output);

  }


  public List<ConverterException> getErrors() {
    return errors;
  }
}


