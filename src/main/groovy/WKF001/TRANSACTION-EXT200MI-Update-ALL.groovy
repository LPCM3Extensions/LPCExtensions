/*
 ***************************************************************
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

 import groovy.lang.Closure

 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 
/*
 * Modification area - M3
 * Name        EXT200MI.Update
 * Type        Transaction
 * Description Authorisation status - Update
 *
 * Nbr       Date      User         Description
 * WRK-001   20260302  Wyllie Lam   Initial 
 */

 /**
  * Update Purchase Authorisation extension table
 */
  public class Update extends ExtendM3Transaction {
    
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String cono;
  private String ttyp;
  private String puno;
  private String plpn;
  private String plps;
  private String plp2;
  private String appr;
  private String purc;
  private String crid;
  private String asts;
  private String lnam;
  private boolean found;
  
  private int xxcono;
  
  public Update(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
   
  }
  
  public void main() {
    logger.debug("UpdateEXT200 main Start");
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	ttyp = mi.inData.get("TTYP") == null ? '' : mi.inData.get("TTYP").trim();
  	puno = mi.inData.get("PUNO") == null ? '' : mi.inData.get("PUNO").trim();
  	if (puno == "?") {
  	  puno = " ";
  	} 
  	plpn = mi.inData.get("PLPN") == null ? '' : mi.inData.get("PLPN").trim();
  	if (plpn == "?") {
  	  plpn = " ";
  	} 
  	plps = mi.inData.get("PLPS") == null ? '' : mi.inData.get("PLPS").trim();
  	if (plps == "?") {
  	  plps = " ";
  	} 
  	plp2 = mi.inData.get("PLP2") == null ? '' : mi.inData.get("PLP2").trim();
  	if (plp2 == "?") {
  	  plp2 = " ";
  	} 
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
  	purc = mi.inData.get("PURC") == null ? '' : mi.inData.get("PURC").trim();
  	if (purc == "?") {
  	  purc = "";
  	} 
  	crid = mi.inData.get("CRID") == null ? '' : mi.inData.get("CRID").trim();
  	if (crid == "?") {
  	  crid = "";
  	} 
  	lnam = mi.inData.get("LNAM") == null ? '' : mi.inData.get("LNAM").trim();
  	if (lnam == "?") {
  	  lnam = "0";
  	}

  	if (plpn.isEmpty()) { plpn = "0";  }
  	if (plps.isEmpty()) { plps = "0";  }
  	if (plp2.isEmpty()) { plp2 = "0";  }

  	if (cono.isEmpty()) {
  	  xxcono = (Integer)program.LDAZD.CONO;
  	} else {
  	  xxcono = cono.toInteger();
  	}

    // Validate input fields for Transaction type
		if (ttyp.isEmpty()) {
      mi.error("Transaction type must be entered");
      return;
    }
    if (!ttyp.equals("PRL") && !ttyp.equals("PO")) {
      mi.error("Invalid transaction type, either PO or PRL");
      return;
    }
    // validate input fields for Purchase Requisition
    if (ttyp.equals("PRL")) {
		   if (plpn.isEmpty()) {
          mi.error("PR no must be entered");
          return;
       }
		   if (plps.isEmpty()) {
          mi.error("PR sub line no must be entered");
          return;
       }
		   if (plp2.isEmpty()) {
          mi.error("PR sub line no 2 must be entered");
          return;
       }
    }
    // validate input fields for Purchase Order
    if (ttyp.equals("PO")) {
		   if (puno.isEmpty()) {
          mi.error("PO no must be entered");
          return;
       }
    }
	
    if (!asts.isEmpty()) {
      if (!asts.equals("Approved") 
        && !asts.equals("Rejected") 
        && !asts.equals("Sent for approval") 
        && !asts.equals("Awaiting approval")) { 
        mi.error("Invalid authorisation status");
        return;
      }    
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build()
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
    // - validate requisition by
    if (!purc.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", purc);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("Requisition by is invalid.");
        return;
      }
    }
    
    // - validate creator id
    if (!crid.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer dbCMNUSR = queryCMNUSR.getContainer();
      dbCMNUSR.set("JUCONO", 0);
      dbCMNUSR.set("JUDIVI", "");
      dbCMNUSR.set("JUUSID", crid);
      if (!queryCMNUSR.read(dbCMNUSR)) {
        mi.error("Creator ID is invalid.");
        return;
      }
    }
        
    DBAction queryEXT200 = database.table("EXT200").index("00").build();
    DBContainer dbEXT200 = queryEXT200.getContainer();
    dbEXT200.set("EXCONO", xxcono);
    dbEXT200.set("EXTTYP", ttyp);
    dbEXT200.set("EXPUNO", puno);
    dbEXT200.set("EXPLPN", plpn.toInteger());
    dbEXT200.set("EXPLPS", plps.toInteger());
    dbEXT200.set("EXPLP2", plp2.toInteger());
    if (!queryEXT200.readLock(dbEXT200, updateCallBack)) {
      mi.error("Record does not exists in EXT200");
      return;
    }
  }
  
  /**
   * updateCallBack - Callback function to update EXTAPR table
   *
   */   
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    
	  ZoneId zid = ZoneId.of("Australia/Sydney"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
    Date systemDate = new Date();
    long timestamp  = systemDate.getTime();    
    
    if (!asts.isEmpty()) {
      lockedResult.set("EXASTS", asts);
    }
    if (!appr.isEmpty()) {
      lockedResult.set("EXAPPR", appr);
    } else {
      if (asts.equals("Sent for approval")) {
        lockedResult.set("EXAPPR", "");
      }
    }
    if (!purc.isEmpty()) {
      lockedResult.set("EXPURC", purc);
    }
    if (!crid.isEmpty()) {
      lockedResult.set("EXCRID", crid);
    }
    if (!lnam.isEmpty()) {
      lockedResult.set("EXLNAM", lnam.toDouble());
    }    
    lockedResult.set("EXCHNO", lockedResult.get("EXCHNO").toString().toInteger() +1);
    lockedResult.set("EXCHID", program.getUser());
    lockedResult.set("EXLMDT", currentDate);
    lockedResult.set("EXLMTS", timestamp);
    lockedResult.update();

  }
}