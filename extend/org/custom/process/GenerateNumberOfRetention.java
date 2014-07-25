package org.custom.process;
/**
 * Genera el comprobante de retencion para los documentos
 * que ya tienen retenciones
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.compiere.model.MInvoice;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;
/**
 * 
 * @author Yamel Senih
 * @date 03/07/2012
 *
 */
public class GenerateNumberOfRetention extends SvrProcess {

	/**	Begin Date							*/
	private Timestamp	p_BeginDate		= null;
	/**	End Date							*/
	private Timestamp	p_EndDate		= null;
	/**	Retention Date						*/
	private Timestamp	p_RetentionDate	= null;
	/**	Type Retention						*/
	private String		p_TypeRetention	= null;
	/**	Sequence Document					*/
	private int		v_Sequence		= 0;
	/**	Retention No						*/
	private String		v_RetentionNo	= null;
	/**	Columna Identificador de Nro de Comprobante	*/
	private String		v_XX_Nro		= null;
	/**	Columna Identificador de Procesado			*/
	private String		v_XX_Procesado		= null;
	/**	Columna Identificador de Fecha del Comprobante	DocStatus IN ('CO', 'CL') AND */
	private String		v_XX_Fecha		= null;
	/**	Type Retention						*/
	private final String ISLR = "IS";
	private final String IVA = "IV";
	@Override
	protected void prepare() {
		ProcessInfoParameter[] params = getParameter();
		for (ProcessInfoParameter para : params) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if(name.equals("DateDoc")){
				p_BeginDate = (Timestamp) para.getParameter();
				p_EndDate = (Timestamp) para.getParameter_To();	
			} else if(name.equals("RetentionDate"))
				p_RetentionDate = (Timestamp) para.getParameter();
			 else if(name.equals("XX_TipoRetencion"))
				p_TypeRetention = (String) para.getParameter();
		}
		
		/**	Sequence Document	*/
		SimpleDateFormat fmt=new SimpleDateFormat("yyyyMM");
		v_RetentionNo = fmt.format(p_RetentionDate);
		/** Changed By Jorge Colmenarez 2014-05-23
		 * 	revome character "#" because present error 
		 * 	"Convertion = s, Flags = #" in Windows XP 
		 *  No Support this argument in the function. **/
		String temp = String.format("%1$" + 8 + "s", "");
		/** End Changed By Jorge Colmenarez 2014-05-23 **/
		v_RetentionNo += temp.replace(' ', '0');
		//	Retention
		
		/**	Set Values Columns	*/
		if(ISLR.equals(p_TypeRetention)){
			v_XX_Nro = new String("XX_Nro_ComprobanteISLR");
			v_XX_Procesado = new String("XX_ProcesadoISLR");
			v_XX_Fecha = new String("XX_Fecha_ComprobanteISLR");
		} else if(IVA.equals(p_TypeRetention)){
			v_XX_Nro = new String("XX_Nro_Comprobante");
			v_XX_Procesado = new String("XX_Procesado");
			v_XX_Fecha = new String("XX_Fecha_Comprobante");
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		/**
		 * Se consulta el valor maximo de la secuencia numerica de los 
		 * comprobantes
		 */
		 
		String sqlDocNo = new String("SELECT " +
				"CASE " +
				"	WHEN max(substr(fac." + v_XX_Nro + ", 7, 8)) IS NULL " +
				"		THEN '0' " +
				"	ELSE max(substr(fac." + v_XX_Nro + ", 7, 8)) END " +
				"FROM C_Invoice fac " +
				"INNER JOIN C_InvoiceLine lfac ON(lfac.C_Invoice_ID = fac.C_Invoice_ID) " +
				"WHERE lfac.XX_TipoRetencion = ?");
		
		v_Sequence = DB.getSQLValue(get_Trx(), sqlDocNo, p_TypeRetention);
		
		log.fine("Max sequence SQL: " + sqlDocNo);
		
		v_Sequence ++;
		
		log.fine("Next Sequence: " + v_Sequence);
		
		/**
		 * Se buscan todos los documentos que estÃ©n completados o cerrados, que no se
		 * le hayan procesado la retenciÃ³n de I.V.A o I.S.L.R y que se encuentren en el 
		 * rango de fecha seleccionado en el proceso
		 */
		PreparedStatement pstmt = null;
		StringBuffer sql = new StringBuffer("SELECT fac.* " +
				"FROM C_Invoice fac " +
				"INNER JOIN C_InvoiceLine lfac ON(lfac.C_Invoice_ID = fac.C_Invoice_ID) " + 
				"WHERE ");
				//	Indentifiers
				sql.append("fac." + v_XX_Nro + " IS NULL ");
				sql.append("AND fac." + v_XX_Procesado + " = 'N' ");
				//	End
				sql.append("AND fac.DateAcct >= " + DB.TO_DATE(p_BeginDate) + " AND fac.DateAcct <=" + DB.TO_DATE(p_EndDate) + " " +
				"AND fac.DocStatus IN ('CO', 'CL') " +
				"AND lfac.XX_TipoRetencion = '" + p_TypeRetention + "' " +
				"AND lfac.linetotalamt <>0");
		
		//System.err.println(DB.TO_DATE(p_BeginDate));
		
		log.fine("Invoice Retention SQL: " + sql);
		
		pstmt = DB.prepareStatement(sql.toString(), get_TrxName());

		ResultSet rs = pstmt.executeQuery();
		
		int created = 0;
		
		if(rs != null){
			MInvoice invoice = null;
			while(rs.next()){
				
				String seqTempLength = String.valueOf(v_Sequence);
				
				invoice = new MInvoice(getCtx(), rs, get_Trx());
				String tmpNro = v_RetentionNo.substring(0, v_RetentionNo.length() - seqTempLength.length()) + v_Sequence;
				//	Identifiers
				invoice.set_Value(v_XX_Nro, tmpNro);
				//System.err.println(v_XX_Fecha);
				invoice.set_Value(v_XX_Fecha, p_RetentionDate);
				invoice.set_Value(v_XX_Procesado, true);
				//	End
				invoice.save(get_Trx());
				//	Add Log Trace
				addLog(Msg.translate(getCtx(), "DocumentNo") + " # " + invoice.getDocumentNo() + " - " + 
						Msg.translate(getCtx(), v_XX_Nro) + " # " + tmpNro);
				//	Inc Sequence
				v_Sequence++;
				
				created++;
			}
		}
		return "@Created@ = " + created;
	}	
}