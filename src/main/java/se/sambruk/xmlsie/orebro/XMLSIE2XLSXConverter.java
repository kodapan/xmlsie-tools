package se.sambruk.xmlsie.orebro;

import org.apache.poi.ss.examples.ToCSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sambruk.xmlsie.Validator;
import se.sie.xml.SIE;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sie.xml.SIE.Accounting.Accounts.Account;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry.LedgerEntry;
import se.sie.xml.SIE.AccountsPayable.Suppliers.Supplier;
import se.sie.xml.SIE.AccountsPayable.Invoices.Invoice;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 * @author kalle
 * @since 2016-09-04 18:33
 */
public class XMLSIE2XLSXConverter {

  private static Logger log = LoggerFactory.getLogger(XMLSIE2XLSXConverter.class);

  private static class Row {
    private Journal journal;
    private Supplier supplier;
    private Invoice invoice;
    private Account account;
    private LedgerEntry ledgerEntry;

    public void write(Writer csv) throws IOException {
      csv.write(journal.getName());
      csv.write("\t");
      csv.write(supplier.getSupplierName());
      csv.write("\t");
      csv.write(supplier.getSupplierOrganizationalNumber());
      csv.write("\t");
      csv.write(invoice.getInternalId());
      csv.write("\t");
      csv.write(String.valueOf(account.getId()));
      if (account.getName() != null) {
        csv.write(" ");
        csv.write(String.valueOf(account.getName()));
      }
      csv.write("\t");
      csv.write(String.valueOf(ledgerEntry.getAmount()));
      csv.write("\n");
    }
  }


  public void convert(Reader xml, Writer csv) throws Exception {
    Unmarshaller unmarshaller = JAXBContext.newInstance(SIE.class).createUnmarshaller();
    SIE sie = (SIE) unmarshaller.unmarshal(xml);
    convert(sie, csv);
  }

  public void convert(SIE sie, Writer csv) throws Exception {

    Validator validator = new Validator();
    if (!validator.validate(sie)) {
      throw new RuntimeException("Validation of incoming SIE failed!\n" + validator.getValidationErrors());
    }

    List<Row> rows = new ArrayList<>();

    Map<BigInteger, SIE.Accounting.Accounts.Account> accounts = new HashMap<>();
    for (Account account : sie.getAccounting().getAccounts().getAccount()) {
      accounts.put(account.getId(), account);
    }

    Map<String, Supplier> suppliers = new HashMap<>();
    for (Supplier supplier : sie.getAccountsPayable().getSuppliers().getSupplier()) {
      suppliers.put(supplier.getSupplierId(), supplier);
    }

    for (FinancialYear financialYear : sie.getAccounting().getFinancialYears().getFinancialYear()) {
      for (Journal journal : financialYear.getJournals().getJournal()) {
        String payer = journal.getName();
        for (JournalEntry journalEntry : journal.getJournalEntry()) {

          Invoice invoice = null;
          for (Invoice searchInvoice : sie.getAccountsPayable().getInvoices().getInvoice()) {
            if (searchInvoice.getJournalInfo().getJournalEntryId().equals(journalEntry.getId())
                && searchInvoice.getJournalInfo().getJournalId().equals(journal.getId())
                && searchInvoice.getJournalInfo().getFinancialYear().equals(financialYear.getStart())) {
              invoice = searchInvoice;
              break;
            }
          }

          if (invoice == null) {
            throw new NullPointerException("Invoice not found for journal entry: " + journalEntry);
          }

          Supplier supplier = suppliers.get(invoice.getSupplierId());

          for (LedgerEntry ledgerEntry : journalEntry.getLedgerEntry()) {
            Account account = accounts.get(ledgerEntry.getAccountId());

            Row row = new Row();
            row.journal = journal;
            row.supplier = supplier;
            row.invoice = invoice;
            row.account = account;
            row.ledgerEntry = ledgerEntry;
            rows.add(row);

          }
        }
      }
    }

    csv.write("Payer\t");
    csv.write("Supplier name\t");
    csv.write("Supplier organization number\t");
    csv.write("Invoice internal identity\t");
    csv.write("Account\t");
    csv.write("Amount\n");

    for (Row row : rows) {
      row.write(csv);
    }

  }


}
