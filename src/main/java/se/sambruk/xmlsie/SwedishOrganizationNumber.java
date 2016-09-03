package se.sambruk.xmlsie;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author kalle
 * @since 2016-09-02 21:20
 */
public class SwedishOrganizationNumber {

  public static boolean isSwedishOrganizationNumber(String organizationNumber) {
    String normalized = organizationNumber.replaceAll("\\D+", "");
    if (normalized.length() != 10) {
      return false;
    }
    return Integer.valueOf(normalized.substring(9)) == Luhn.checkSum(normalized);
  }

  /**
   * This method will detect organization numbers that belong to Swedish sole traders.
   * False positive matches are are possibility, but false negatives are not.
   *
   * @param organizationNumber
   * @return true if organization number might belong to a Swedish sole trader.
   */
  public static boolean isSoleTrader(String organizationNumber) {
    if (!isSwedishOrganizationNumber(organizationNumber)) {
      return false;
    }

    String normalized = organizationNumber.replaceAll("\\D+", "");
    if (normalized.length() != 10) {
      return false;
    }

    int month = Integer.valueOf(organizationNumber.substring(2,4));
    if (month == 0 || month > 12) {
      return false;
    }

    int day = Integer.valueOf(organizationNumber.substring(4,6));
    if (day == 0 || day > 31) {
      // todo assert day exist in month that yeat. leap years and so on!
      // todo gregorian calendar
      return false;
    }

    return true;
  }



}
