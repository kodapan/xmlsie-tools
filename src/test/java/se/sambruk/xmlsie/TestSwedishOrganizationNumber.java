package se.sambruk.xmlsie;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author kalle
 * @since 2016-09-03 13:25
 */
public class TestSwedishOrganizationNumber {

  @Test
  public void testOrganizationNumber() {


    Assert.assertTrue(SwedishOrganizationNumber.isSwedishOrganizationNumber("802428-2785"));
    Assert.assertTrue(SwedishOrganizationNumber.isSwedishOrganizationNumber("8024282785"));
    Assert.assertTrue(SwedishOrganizationNumber.isSwedishOrganizationNumber("SE8024282785"));

    Assert.assertTrue(SwedishOrganizationNumber.isSwedishOrganizationNumber("750730-4652"));

    Assert.assertFalse(SwedishOrganizationNumber.isSwedishOrganizationNumber("802428-278"));
    Assert.assertFalse(SwedishOrganizationNumber.isSwedishOrganizationNumber("802428-2786"));

  }

  @Test
  public void testSoleTrader() {

    Assert.assertFalse(SwedishOrganizationNumber.isSoleTrader("802428-2785"));
    Assert.assertTrue(SwedishOrganizationNumber.isSoleTrader("750730-4652"));


  }


}
