package se.sambruk.xmlsie;

import se.sie.xml.SIE;
import se.sie.xml.SIE.Accounting.Accounts.Account;
import se.sie.xml.SIE.AccountsPayable.Suppliers.Supplier;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal;
import se.sie.xml.SIE.Accounting.FinancialYears.FinancialYear.Journals.Journal.JournalEntry;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;

/**
 * @author kalle
 * @since 2016-06-19 16:39
 */
public class Validator {


  private LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
  private LinkedHashSet<String> validationWarnings = new LinkedHashSet<>();

  public boolean validate(String xml) throws Exception {
    return validate(new StringReader(xml));
  }

  public boolean validate(Reader xml) throws Exception {
    Unmarshaller unmarshaller = JAXBContext.newInstance(SIE.class).createUnmarshaller();
    SIE sie;
    try {
      sie = (SIE) unmarshaller.unmarshal(xml);
    } catch (Exception e) {
      validationErrors.add(e.getMessage());
      return false;
    }
    return validate(sie);
  }

  public boolean validate(SIE sie) throws Exception {


    // mainly prepare maps to look up coupled objects, but also assert identities


    Map<BigInteger, Account> accounts = new HashMap<>();
    if (sie.getAccounting() == null || sie.getAccounting().getAccounts() == null || sie.getAccounting().getAccounts().getAccount() == null || sie.getAccounting().getAccounts().getAccount().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><Accounting><Accounts>");
    } else {
      for (Account account : sie.getAccounting().getAccounts().getAccount()) {
        if (account.getId() == null) {
          validationErrors.add("Missing <Account><Id> in " + marshall(account));
        } else {
          Account existingAccount = accounts.put(account.getId(), account);
          if (existingAccount != null) {
            validationErrors.add("Not unique <Account><Id> in " + marshall(account));
          } else {
            // todo assert unique identities that are not nothing
          }
        }
      }
    }

    Map<String, Supplier> suppliers = new HashMap<>();
    if (sie.getAccountsPayable() == null || sie.getAccountsPayable().getSuppliers() == null || sie.getAccountsPayable().getSuppliers().getSupplier() == null || sie.getAccountsPayable().getSuppliers().getSupplier().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><AccountsPayable><Suppliers>");
    } else {
      for (Supplier supplier : sie.getAccountsPayable().getSuppliers().getSupplier()) {
        if (supplier.getSupplierId() == null) {
          validationErrors.add("Missing <Supplier><Id> in " + marshall(supplier));
        } else {
          // todo assert unique identities that are not nothing
          Supplier existingSupplier = suppliers.put(supplier.getSupplierId(), supplier);
          if (existingSupplier != null) {
            validationErrors.add("Not unique <Supplier><Id> in " + marshall(supplier));
          }
        }
      }
    }

    // todo assert financial years do not overlap

    Map<Long, Map<String, Map<String, JournalEntry>>> journalEntries = new HashMap<>();
    if (sie.getAccounting() == null || sie.getAccounting().getFinancialYears() == null || sie.getAccounting().getFinancialYears().getFinancialYear() == null || sie.getAccounting().getFinancialYears().getFinancialYear().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><Accounting><FinancialYears>");
    } else {
      for (SIE.Accounting.FinancialYears.FinancialYear financialYear : sie.getAccounting().getFinancialYears().getFinancialYear()) {
        Map<String, Map<String, JournalEntry>> financialYearMap = new HashMap<>();
        // todo assert unique identities that are not nothing
        Object existingFinancialYearMap = journalEntries.put(financialYear.getStart().toGregorianCalendar().getTimeInMillis(), financialYearMap);
        if (existingFinancialYearMap != null) {
          validationErrors.add("Not unique <FinancialYear><Start> in " + marshall(financialYear));
        }
        for (Journal journal : financialYear.getJournals().getJournal()) {
          if (journal.getId() == null) {
            validationWarnings.add("Missing <Journal><Id> in " + marshall(journal));

          } else {
            Map<String, JournalEntry> journalMap = new HashMap<>();
            // todo assert unique identities that are not nothing
            Object existingJournalMap = financialYearMap.put(journal.getId(), journalMap);
            if (existingJournalMap != null) {
              validationErrors.add("Not unique <FinancialYear><Journals><Journal><Id> in " + marshall(journal));
            }
            for (JournalEntry journalEntry : journal.getJournalEntry()) {
              if (journalEntry.getId() == null) {
                validationWarnings.add("Missing <JournalEntry><Id> in " + marshall(journalEntry));

              } else {
                JournalEntry existingJournalEntry = journalMap.put(journalEntry.getId(), journalEntry);
                if (existingJournalEntry != null) {
                  validationErrors.add("Not unique <FinancialYear><Journals><Journal><JournalEntry><Id> in " + marshall(journalEntry));
                }
              }
            }
          }
        }
      }
    }


    // validation

    if (sie.getCompany() == null) {
      validationErrors.add("Missing <SIE><Company>");
    } else {
      if (sie.getCompany().getOrganizationalnumber() == null) {
        validationErrors.add("Missing <SIE><Company organizationalnumber>");
      } else {
        // todo Luhn check
      }

      if (sie.getCompany().getName() == null) {
        validationErrors.add("Missing <SIE><Company name>");
      }
      if (sie.getCompany().getAddressLine1() == null) {
        validationErrors.add("Missing <SIE><Company addressLine1>");
      }
      if (sie.getCompany().getPostcode() == null) {
        validationErrors.add("Missing <SIE><Company postcode>");
      }
      if (sie.getCompany().getCity() == null) {
        validationErrors.add("Missing <SIE><Company city>");
      }
      if (sie.getCompany().getCountryCode() == null) {
        validationErrors.add("Missing <SIE><Company countryCode>");
      }
    }


    // financial years, journals, journal entries and ledger entries
    if (sie.getAccounting() == null || sie.getAccounting().getFinancialYears() == null || sie.getAccounting().getFinancialYears().getFinancialYear() == null || sie.getAccounting().getFinancialYears().getFinancialYear().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><Accounting><FinancialYears>");
    } else {
      for (SIE.Accounting.FinancialYears.FinancialYear financialYear : sie.getAccounting().getFinancialYears().getFinancialYear()) {
        if (financialYear.getJournals() == null || financialYear.getJournals().getJournal() == null || financialYear.getJournals().getJournal().isEmpty()) {
          validationErrors.add("Missing or empty <SIE><Accounting><FinancialYears><FinacialYear><Journals>");

        } else {
          for (Journal journal : financialYear.getJournals().getJournal()) {

            // todo sanity check text
            // todo check id


            if (journal.getJournalEntry() == null || journal.getJournalEntry().isEmpty()) {
              validationErrors.add("Missing or empty <SIE><Accounting><FinancialYears><Journals><Journal><JournalEntry> records");

            } else {
              for (JournalEntry journalEntry : journal.getJournalEntry()) {

                // todo sanity check dates
                // todo sanity check text

                if (journalEntry.getLedgerEntry() == null || journalEntry.getLedgerEntry().isEmpty()) {
                  validationErrors.add("Missing or empty <SIE><Accounting><FinancialYears><Journals><Journal><JournalEntry><LedgerEntry> records");

                } else {
                  for (JournalEntry.LedgerEntry ledgerEntry : journalEntry.getLedgerEntry()) {

                    SIE.Accounting.Accounts.Account account = accounts.get(ledgerEntry.getAccountId());
                    if (account == null) {
                      validationErrors.add("Non existing <Account><Id> reference <AccountId> in " + marshall(ledgerEntry));

                    }

                    // todo sanity check dates
                    // todo sanity check text

                  }
                }
              }
            }
          }
        }
      }
    }

    // invoices
    if (sie.getAccountsPayable() == null || sie.getAccountsPayable().getInvoices() == null || sie.getAccountsPayable().getInvoices().getInvoice() == null || sie.getAccountsPayable().getInvoices().getInvoice().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><AccountsPauable><Invoices>");

    } else {
      for (SIE.AccountsPayable.Invoices.Invoice invoice : sie.getAccountsPayable().getInvoices().getInvoice()) {

        if (invoice.getSupplierId() == null) {
          validationErrors.add("Missing <SIE><AccountsPauable><Invoices><Invoice><SupplierId> in " + marshall(invoice));

        } else {

          // todo assert unique identities that are not nothing

          Supplier supplier = suppliers.get(invoice.getSupplierId());
          if (supplier == null) {
            validationErrors.add("Non existing <Supplier><Id> reference <SupplierId> in " + marshall(invoice));

          }
        }

        if (invoice.getJournalInfo() == null) {
          // todo error
        } else {
          Map<String, Map<String, JournalEntry>> financialYearMap = journalEntries.get(invoice.getJournalInfo().getFinancialYear().toGregorianCalendar().getTimeInMillis());
          if (financialYearMap == null) {
            validationErrors.add("Non existing <FinancialYear><Start> reference <FinancialYear> in " + marshall(invoice));
          } else {
            Map<String, JournalEntry> journalMap = financialYearMap.get(invoice.getJournalInfo().getJournalId());
            if (journalMap == null) {
              validationErrors.add("Non existing <Journal><Id> reference <JouranlId> in " + marshall(invoice));

            } else {
              JournalEntry journalEntry = journalMap.get(invoice.getJournalInfo().getJournalEntryId());
              if (journalEntry == null) {
                validationErrors.add("Non existing <JournalEntry><Id> reference <JouranlEntryId> in " + marshall(invoice));

              }
            }
          }

          System.currentTimeMillis();

        }
      }
    }


    // suppliers
    if (sie.getAccountsPayable() == null || sie.getAccountsPayable().getSuppliers() == null || sie.getAccountsPayable().getSuppliers().getSupplier() == null || sie.getAccountsPayable().getSuppliers().getSupplier().isEmpty()) {
      validationErrors.add("Missing or empty <SIE><AccountsPayable><Suppliers>");

    } else {
      for (Supplier supplier : sie.getAccountsPayable().getSuppliers().getSupplier()) {

        if (supplier.getSupplierId() == null) {
          validationErrors.add("Missing <Supplier><SupplierId> in " + marshall(supplier));

        }

        if (supplier.getSupplierOrganizationalNumber() == null) {
          validationErrors.add("Missing <Supplier><SupplierOrganizationalNumber> in " + marshall(supplier));
        } else {
          // todo Luhn
        }

        if (supplier.getSupplierName() == null || supplier.getSupplierName().trim().isEmpty()) {
          validationErrors.add("Missing <Supplier><SupplierName> in " + marshall(supplier));

        }

      }
    }

    return validationErrors.isEmpty();

  }

  private String marshall(Object object) throws JAXBException {
    StringWriter out = new StringWriter(4096);

    JAXBContext.newInstance(SIE.class).createMarshaller().marshal(new JAXBElement(new QName(null, object.getClass().getSimpleName()), object.getClass(), object), out);

    return out.toString();
  }

  public LinkedHashSet<String> getValidationErrors() {
    return validationErrors;
  }

  public LinkedHashSet<String> getValidationWarnings() {
    return validationWarnings;
  }
}
