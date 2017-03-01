package se.sambruk.xmlsie.converter;

import lombok.Data;
import se.sie.xml.SIE;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kalle
 * @since 2017-03-01 20:08
 */
@Data
public class FactoryInvoice {

  private SIE.AccountsPayable.Invoices.Invoice invoice;

  private FactoryPayer payer;
  private String supplierName;
  private String supplierOrganizationNumber;

  private String number;
  private List<FactoryInvoiceAccountAmountPosting> postings = new ArrayList<>();

}
