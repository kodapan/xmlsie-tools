package se.sambruk.xmlsie.converter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kalle
 * @since 2017-03-01 20:08
 */
@Data
public class FactoryJournal {
  private String name;
  private List<FactoryInvoice> invoices = new ArrayList<>();
}
