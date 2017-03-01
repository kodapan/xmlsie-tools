package se.sambruk.xmlsie.converter;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author kalle
 * @since 2017-03-01 20:08
 */
@Data
public class FactoryInvoiceAccountAmountPosting {
  private FactoryAccount account;
  private BigDecimal amount;

  FactoryInvoiceAccountAmountPosting(FactoryAccount account, BigDecimal amount) {
    this.account = account;
    this.amount = amount;
  }
}
