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
 
/*
 * Modification area - M3
 * Name        EXT200MI.Delete
 * Type        Transaction
 * Description Authorisation status - Delete
 *
 * Nbr       Date      User         Description
 * WRK-001   20260302  Wyllie Lam   Initial
 *
 */


 /**
  * Delete Purchase Authorisation extension table row
 */
 public class Delete extends ExtendM3Transaction {
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
  
  private int xxcono;
  
 /*
  * Delete Purchase Authorisation extension table row
 */
  public Delete(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
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
  	  puno = "";
  	}
  	plpn = mi.inData.get("PLPN") == null ? '' : mi.inData.get("PLPN").trim();
  	if (plpn == "?") {
  	  plpn = "0";
  	}
  	plps = mi.inData.get("PLPS") == null ? '' : mi.inData.get("PLPS").trim();
  	if (plps == "?") {
  	  plps = "0";
  	}
  	plp2 = mi.inData.get("PLP2") == null ? '' : mi.inData.get("PLP2").trim();
  	if (plp2 == "?") {
  	  plp2 = "0";
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
  	  xxcono = (Integer)program.LDAZD.CONO;
  	} else {
  	  xxcono = cono.toInteger();
  	}

  	DBAction queryEXT200 = database.table("EXT200").index("00").selection("EXPUNO", "EXPLPN", "EXPLPS", "EXPLP2").build();
    DBContainer ext200 = queryEXT200.getContainer();
    ext200.set("EXCONO", xxcono);
    ext200.set("EXTTYP", ttyp);
    ext200.set("EXPUNO", puno);
    ext200.set("EXPLPN", plpn.toInteger());
    ext200.set("EXPLPS", plps.toInteger());
    ext200.set("EXPLP2", plp2.toInteger());
    if (!queryEXT200.readLock(ext200, deleteCallBack)) {
      mi.error("Record does not exist");
      return;
    }
  }
  
  /**
   * deleteCallBack - Callback function to delete EXT200 table
   *
  */
  Closure<?> deleteCallBack = { LockedResult lockedResult ->

    lockedResult.delete();
  
   
  }
  
}