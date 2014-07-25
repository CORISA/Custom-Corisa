package org.custom.process;

import java.util.logging.Level;

import org.compiere.model.MPayment;
import org.compiere.print.ReportCtl;
import org.compiere.print.ReportEngine;
import org.compiere.print.Viewer;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *	Print Check	
 *	
 *  @author Jorge Colmenarez
 *  @version $Id: PrintCheck.java,v 1.0 2014/07/18 11:53:25 $
 */
public class PrintCheck extends SvrProcess{

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		
		for(ProcessInfoParameter parameter :getParameter()){
			String name = parameter.getParameterName();
			if(parameter.getParameter() == null)
				;
			else if(name.equals("C_Payment_ID"))
				p_C_Payment_ID = parameter.getParameterAsInt();
			
			else 
				log.log(Level.SEVERE ,"Unknown Parameter:" + name);
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		MPayment pay = new MPayment(getCtx(), p_C_Payment_ID,get_Trx());
		pay.set_CustomColumn("IsPrinted",true);
		
		printedReport = ReportCtl.startCheckPrint(getCtx(),p_C_Payment_ID, false);
		
		if(printedReport!=null){
			pay.save(get_Trx());
			new Viewer( printedReport );
			return "@ReportPreview@";
		}
		else 
			return "@GeneratedReportError@";
		
	}

	int p_C_Payment_ID = 0;
	ReportEngine printedReport;

}
