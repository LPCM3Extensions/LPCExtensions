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
 *   THE PROPERTY OF THEIR APPRECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */

 import groovy.lang.Closure
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 import groovy.json.JsonSlurper;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;


/*
 *Modification area - M3
 *Nbr         Date      User id     Description
 *WRK-001     20260302  WLAM        Authorisation status - Add
 *
 */

/**
* - Add Purchase Authorisation extension table row
*/
public class Add extends ExtendM3Transaction {
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
  private String asts = "";
  private String appr = "";
  private String purc;
  private String crid;

  private boolean found;
  

  private int XXCONO;
 
 /*
  * Add Purchase Authorisation extension table row
 */
  public Add(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
	  
  }
  
  public void main() {
    
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

  	if (plpn.isEmpty()) { plpn = "0";  }
  	if (plps.isEmpty()) { plps = "0";  }
  	if (plp2.isEmpty()) { plp2 = "0";  }

  	if (cono.isEmpty()) {
  	  XXCONO = (Integer)program.LDAZD.CONO;
  	} else {
  	  XXCONO = cono.toInteger();
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

  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
		if (asts.isEmpty()) {
      mi.error("Authorisation status must be entered");
      return;
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

    // - validate planned PO 
    if (ttyp.equals("PRL")) {
      DBAction queryMPOPLP = database.table("MPOPLP").index("00").selection("POCONO", "POPLPN", "POPLPS", "POPLP2").build();
      DBContainer MPOPLP = queryMPOPLP.getContainer();
      MPOPLP.set("POCONO", XXCONO);
      MPOPLP.set("POPLPN", plpn.toInteger());
      MPOPLP.set("POPLPS", plps.toInteger());
      MPOPLP.set("POPLP2", plp2.toInteger());
      if (!queryMPOPLP.read(MPOPLP)) {
        mi.error("PO Requisition number invalid");
        return;
      }    
    }
    

    // - validate puno
    if (ttyp.equals("PO")) {
       DBAction queryMPHEAD = database.table("MPHEAD").index("00").selection("IAPUNO").build();
       DBContainer MPHEAD = queryMPHEAD.getContainer();
       MPHEAD.set("IACONO", XXCONO);
       MPHEAD.set("IAPUNO", puno);
       if (!queryMPHEAD.read(MPHEAD)) {    
        mi.error("PO number is invalid.");
        return;
       }
    }

    // - validate authorisation status
    if (!asts.equals("Approved") 
        && !asts.equals("Rejected") 
        && !asts.equals("Sent for approval") 
        && !asts.equals("Awaiting approval")) { 
      mi.error("Invalid authorisation status");
      return;
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
  	
    // - validate requisition by
    if (!purc.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", purc);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Requisition by is invalid.");
        return;
      }
    }
    
    // - validate creator id
    if (!crid.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", crid);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Creator ID is invalid.");
        return;
      }
    }
    

    writeEXT200();
    
  }
  /**
  * writeEXT200 - Write Purchase Authorisation extension table EXT200
  *
  */
  private void writeEXT200() {
  	ZoneId zid = ZoneId.of("Australia/Sydney"); 
    LocalDateTime currentDateTimeNow = LocalDateTime.now(zid);
    int currentDate = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    int currentTime = Integer.valueOf(currentDateTimeNow.format(DateTimeFormatter.ofPattern("HHmmss")));
    //String timestamp = currentDateTimeNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    Date systemDate = new Date();
    long timestamp  = systemDate.getTime();

	  DBAction actionEXT200 = database.table("EXT200").build();
  	DBContainer EXT200 = actionEXT200.getContainer();
  	EXT200.set("EXCONO", XXCONO);
  	EXT200.set("EXTTYP", ttyp);
  	if (!puno.isEmpty()) {
  	  EXT200.set("EXPUNO", puno);
  	}
  	if (!plpn.isEmpty()) {
  	  EXT200.set("EXPLPN", plpn.toInteger());
  	}
  	if (!plps.isEmpty()) {
  	  EXT200.set("EXPLPS", plps.toInteger());
  	}
  	if (!plp2.isEmpty()) {
  	  EXT200.set("EXPLP2", plp2.toInteger());
  	}
  	EXT200.set("EXASTS", asts);
  	if (!appr.isEmpty()) {
  	  EXT200.set("EXAPPR", appr);
  	}  	
  	EXT200.set("EXLMTS", timestamp);
  	EXT200.set("EXRGDT", currentDate);
  	EXT200.set("EXRGTM", currentTime);
  	EXT200.set("EXLMDT", currentDate);
  	EXT200.set("EXCHNO", 0);
  	EXT200.set("EXCHID", program.getUser());
  	if (!purc.isEmpty()) {
  	  EXT200.set("EXPURC", purc);
  	}  	
  	if (!crid.isEmpty()) {
  	  EXT200.set("EXCRID", crid);
  	}  	
  	EXT200.set("EXLNAM", 0);
    actionEXT200.insert(EXT200, recordExists);
	}
	
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
  
}