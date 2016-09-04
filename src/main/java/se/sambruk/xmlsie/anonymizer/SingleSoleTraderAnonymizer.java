package se.sambruk.xmlsie.anonymizer;

import se.sambruk.xmlsie.SwedishOrganizationNumber;
import se.sie.xml.ObjectFactory;
import se.sie.xml.SIE;

import java.util.*;

/**
 * @author kalle
 * @since 2016-09-02 21:10
 */
public class SingleSoleTraderAnonymizer  extends Anonymizer {

  private String soleTraderSupplierName = "Anonymized sole traders";
  private String soleTraderSupplierIdentity = "SOLE_TRADERS";
  private String soleTraderSupplierOrganizationNumber = "000000-0000";

  @Override
  public void anonymize(SIE sie) throws Exception {

    Map<String, SIE.AccountsPayable.Suppliers.Supplier> soleTraders = new HashMap<>();

    // gather all sole trader suppliers
    for (SIE.AccountsPayable.Suppliers.Supplier supplier : sie.getAccountsPayable().getSuppliers().getSupplier()) {
      if (SwedishOrganizationNumber.isSoleTrader(supplier.getSupplierOrganizationalNumber())) {
        soleTraders.put(supplier.getSupplierId(), supplier);
      }
    }

    List<SIE.AccountsPayable.Invoices.Invoice> soleTraderInvoices = new ArrayList<>();

    // gather all invoices from sole traders
    for (SIE.AccountsPayable.Invoices.Invoice invoice : sie.getAccountsPayable().getInvoices().getInvoice()) {
      SIE.AccountsPayable.Suppliers.Supplier soleTrader = soleTraders.get(invoice.getSupplierId());
      if (soleTrader != null) {
        soleTraderInvoices.add(invoice);
      }
    }


    SIE.AccountsPayable.Suppliers.Supplier soleTraderSupplier = new ObjectFactory().createSIEAccountsPayableSuppliersSupplier();
    soleTraderSupplier.setSupplierName(soleTraderSupplierName);
    soleTraderSupplier.setSupplierId(soleTraderSupplierIdentity);
    soleTraderSupplier.setSupplierOrganizationalNumber(soleTraderSupplierOrganizationNumber);

    sie.getAccountsPayable().getSuppliers().getSupplier().add(soleTraderSupplier);

//    for (SIE.AccountsPayable.Suppliers.Supplier supplier : soleTraders.values()) {
//      System.out.println(supplier.getSupplierOrganizationalNumber());
//      if (supplier.getSupplierOrganizationalNumber().startsWith("55")) {
//        System.currentTimeMillis();
//      }
//    }
//
    sie.getAccountsPayable().getSuppliers().getSupplier().removeAll(soleTraders.values());

    for (SIE.AccountsPayable.Invoices.Invoice invoice : soleTraderInvoices) {
      invoice.setSupplierId(soleTraderSupplier.getSupplierId());
    }

  }
}
