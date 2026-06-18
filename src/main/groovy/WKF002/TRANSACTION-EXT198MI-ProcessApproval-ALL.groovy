/***************************************************************
 *                                                             *
 *                           NOTICE                            *
 *                                                             *
 *   THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS             *
 *   CONFIDENTIAL INFORMATION OF INFOR AND/OR ITS AFFILIATES   *
 *   OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED WITHOUT PRIOR  *
 *   WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND       *
 *   ADAPT THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH  *
 *   THE TERMS OF THEIR SOFTWARE LICENSE AGREEMENT.            *
 *   ALL OTHER RIGHTS RESERVED.                                *
 *                                                             *
 *   (c) COPYRIGHT 2020 INFOR.  ALL RIGHTS RESERVED.           *
 *   THE WORD AND DESIGN MARKS SET FORTH HEREIN ARE            *
 *   TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR          *
 *   AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS        *
 *   RESERVED.  ALL OTHER TRADEMARKS LISTED HEREIN ARE         *
 *   THE PROPERTY OF THEIR RESPECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */
 
 import groovy.lang.Closure;
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 
/*
* Modification area - M3
* Name        EXT198MI.ProcessApproval
* Type        Transaction
* Description Supplier invoice approval run
*
* Nbr         Date      User id     Description
* WRK-001     20260302  WLAM        Initial 
*/

/**
* ProcessApproval - Processing supplier invoices ready for approval to pay
*/
public class ProcessApproval extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;

  private int XXCONO;
  private int currentDate;
  private int currentTime;
  private String accountStatus;
  
  boolean allStatusesOk;
  
  private List lstToBeApproved;
  
  public ProcessApproval(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
    this.miCaller = miCaller;
    this.logger = logger;
    this.program = program;
  }
  
  public void main() {

    XXCONO= program.LDAZD.CONO;
    
    accountStatus = "";

    currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    
    lstToBeApproved = new ArrayList();
    
    ExpressionFactory expression = database.getExpressionFactory("FPLEDG");

    DBAction queryFPLEDG = database.table("FPLEDG").index("12").selection("EPDIVI", "EPSUNO", "EPSINO", "EPINYR", "EPAPRV").build();
    DBContainer dbFPLEDG = queryFPLEDG.getContainer();
    dbFPLEDG.set("EPCONO", XXCONO);
   
    queryFPLEDG.readAll(dbFPLEDG, 1, 9999, lstFPLEDG);
    
    logger.debug("lstToBeApproved.size=" + lstToBeApproved.size());
    for (int i=0;i<lstToBeApproved.size();i++) {
      Map<String, String> record = (Map<String, String>) lstToBeApproved[i];
		  String divi = record.DIVI.trim();
		  String suno = record.SUNO.trim();
		  String sino = record.SINO.trim();
		  String inyr = record.INYR.trim();
		  logger.debug("call APS110MI SINO=" + sino); 

		  Map<String, String>  params = [ "DIVI": divi, "SUNO": suno, "SINO": sino, "INYR": inyr];
      Closure<?> callback = {
        Map<String, String> response ->
      }
      miCaller.call("APS110MI","ApproveInvoice", params, callback);
    }
  }

  /**
   * lstFPLEDG - Callback function to return FPLEDG records
   *
  */
  Closure<?> lstFPLEDG = { DBContainer FPLEDG ->
    String divi = FPLEDG.get("EPDIVI").toString().trim();
    String suno = FPLEDG.get("EPSUNO").toString().trim();
    String sino = FPLEDG.get("EPSINO").toString().trim();
    String inyr = FPLEDG.get("EPINYR").toString().trim();
    String aprv = FPLEDG.get("EPAPRV").toString().trim();
    
    allStatusesOk = true;
    DBAction queryFGINHE = database.table("FGINHE").index("00").selection("F4INS0").build();
    DBContainer dbFGINHE = queryFGINHE.getContainer();
    dbFGINHE.set("F4CONO", XXCONO);
    dbFGINHE.set("F4DIVI", divi);
    dbFGINHE.set("F4SUNO", suno);
    dbFGINHE.set("F4SINO", sino);
    dbFGINHE.set("F4INYR", inyr.toInteger());
    
    if (queryFGINHE.read(dbFGINHE)) {
      String ins0 = dbFGINHE.get("F4INS0").toString().trim();
      if (ins0 == "33334") {
        Map<String, String> map = [DIVI: divi, SUNO: suno, SINO: sino, INYR: inyr];
        lstToBeApproved.add(map);
      }
      // Workaround for M3 error where all lines are set to 33334 but header is stuck on 33333
      if (ins0 == "33333") {
        DBAction queryFGINLI = database.table("FGINLI").index("10").selection("F5INS5").build();
        DBContainer dbFGINLI = queryFGINLI.getContainer();
        dbFGINLI.set("F5CONO", XXCONO);
        dbFGINLI.set("F5DIVI", divi);
        dbFGINLI.set("F5SUNO", suno);
        dbFGINLI.set("F5SINO", sino);
        dbFGINLI.set("F5INYR", inyr.toInteger());
        dbFGINLI.set("F5INS5", "3");
        queryFGINLI.readAll(dbFGINLI, 6, 1, lstFGINLI);
         //If no lines are found waiting for account correction
        if (accountStatus.isBlank()) {
          Map<String, String> map = [DIVI: divi, SUNO: suno, SINO: sino, INYR: inyr];
          lstToBeApproved.add(map);
        }
      }
    }
    
  }
  
  /*
   * lstFGINLI - Callback function to return FGINLI records
   *
  */
  Closure<?> lstFGINLI = { DBContainer dbFGINLI ->
      accountStatus = dbFGINLI.get("F5INS5").toString().trim();
  }


}