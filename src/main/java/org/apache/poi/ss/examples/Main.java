package org.apache.poi.ss.examples;

import java.io.File;

/**
 * @author kalle
 * @since 2016-06-18 23:31
 */
public class Main {

  public static void main(String[] args) throws Exception {

    File xls = new File("/tmp/Leverantörsfakturor+2016-05.xlsx");
    File csv = new File("/tmp/");

    ToCSV toCSV = new ToCSV();
    toCSV.convertExcelToCSV(xls.getAbsolutePath(), csv.getAbsolutePath(), "\t");


  }

}
