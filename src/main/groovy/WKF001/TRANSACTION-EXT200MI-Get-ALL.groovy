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
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import groovy.json.JsonSlurper;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;

/*
 *Modification area - M3
 *Nbr       Date      User id     Description
 *WRK-001   20260302  WLAM        Authorisation status - Get
 *
*/

 /**
  * Get Purchase Authorisation extension table row
 */
 public class Get extends ExtendM3Transaction {
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
  
  private int XXCONO;
   
 /*
  * Get Purchase Authorisation extension table row
 */
  public Get(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
  	
   	if (plpn.isEmpty()) { plpn = "0";  }
   	if (plps.isEmpty()) { plps = "0";  }
  	if (plp2.isEmpty()) { plp2 = "0";  } 	
  	
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

  	if (cono.isEmpty()) {
  	  XXCONO = (Integer)program.LDAZD.CONO;
  	} else {
  	  XXCONO = cono.toInteger();
  	}

    DBAction query = database.table("EXT200").index("00").selection("EXCONO", "EXPUNO", "EXPLPN", "EXPLPS", "EXPLP2", "EXAPPR", "EXASTS", "EXLMTS", "EXPURC", "EXCRID", "EXLNAM").build();
    DBContainer container = query.getContainer();
    container.set("EXCONO", XXCONO);
    container.set("EXTTYP", ttyp);
    container.set("EXPUNO", puno);
    container.set("EXPLPN", plpn.toInteger());
    container.set("EXPLPS", plps.toInteger());
    container.set("EXPLP2", plp2.toInteger());
    if (query.read(container)) {
      mi.outData.put("CONO", XXCONO.toString());
      mi.outData.put("PUNO", container.get("EXPUNO").toString());
      mi.outData.put("TTYP", container.get("EXTTYP").toString());
      mi.outData.put("PLPN", container.get("EXPLPN").toString());
      mi.outData.put("PLPS", container.get("EXPLPS").toString());
      mi.outData.put("PLP2", container.get("EXPLP2").toString());
      mi.outData.put("APPR", container.get("EXAPPR").toString());
      mi.outData.put("ASTS", container.get("EXASTS").toString());
      mi.outData.put("PURC", container.get("EXPURC").toString());
      mi.outData.put("CRID", container.get("EXCRID").toString());
      mi.outData.put("LMTS", container.get("EXLMTS").toString());
      mi.outData.put("LNAM", container.get("EXLNAM").toString());
      mi.write();
    } else {
      mi.error("Record does not exist in EXT200.");
      return;
    }
  }
  
}