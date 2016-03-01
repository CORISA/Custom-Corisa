/**
 * @author Carlos Parada
 * @Date 23/03/2012
 */
package org.custom.report;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.compiere.util.DB;
import org.compiere.util.Trx;

/**
 * @author Carlos Parada
 * @Date 23/03/2012
 * Clase Modelo Reporte ARCV
 */
@SuppressWarnings("unused")
public class MXX_ARCV {

	
	/**
	 * @author Carlos Parada
	 * @Date 23/03/2012
	 * Constructor Inicializa Las Variables del Reporte
	 */
	public MXX_ARCV(Integer m_AD_PInstance_ID,Integer m_Ad_Client_ID,Integer m_Ad_Org_ID,Integer m_C_BPartner_ID,Timestamp m_DateInvoiced,Timestamp m_DateInvoiced_To,Timestamp m_DateAcct,Timestamp m_DateAcct_To) {
		// TODO Auto-generated constructor stub
		this.m_AD_PInstance_ID=m_AD_PInstance_ID;
		this.m_Ad_Client_ID=m_Ad_Client_ID;
		this.m_Ad_Org_ID=m_Ad_Org_ID;
		this.m_C_BPartner_ID=m_C_BPartner_ID;
		this.m_DateInvoiced=m_DateInvoiced;
		this.m_DateInvoiced_To=m_DateInvoiced_To;
		this.m_DateAcct=m_DateAcct;
		this.m_DateAcct_To=m_DateAcct_To;	
	}

	/** @author Carlos Parada
	 * @throws SQLException 
	 * @Date 23/03/2012
	 * @Return boolean 
	 * Busca los proveedores a los que se les retuvo impuesto sobre la renta en el periodo especificado retorna verdadero en caso de encontrar informacion, de lo contrario no retorna ningun registro
	 */
	public boolean LoadDataReport() throws SQLException
	{
		if(InsertXX_ARCV()!=null)
			return true;
		else
			return false;	
	}
	/**@author Carlos Parada
	 * @date 26/03/2012
	 * @return Resultset
	 * Devuelve resultset con los proveedores que se les aplico retencion de impuesto sobre la renta
	 * */
	private ResultSet InsertXX_ARCV() throws SQLException
	{
		StringBuffer sql = new StringBuffer();
		getSql(sql);
		String trxName = "XXARCV";
		Trx trx = Trx.get(trxName);
		PreparedStatement ps = null;
		ps = DB.prepareStatement(sql.toString(),trx);
		ResultSet rs = ps.executeQuery();
		trx.commit();
		return rs;
	}
	/**
	 * @autor Carlos Parada
	 * @date 30/03/2012
	 * @return void
	 * arma la consulta sql para cargar la data del reporte
	 * */
	private void getSql(StringBuffer sql)
	{
	sql.delete(0, sql.length());
	sql.append("Insert into T_XX_ARCV"+
					   "(AD_Client_ID,"+
					   "AD_Org_ID,"+
					   "AD_PInstance_ID," +
					   "DateAcct," +
					   "DateAcct_To," +
					   "DateInvoiced," +
					   "DateInvoiced_To,"+
					   "C_BPartner_ID)");
			sql.append("Select Distinct "+
						m_Ad_Client_ID+","+
						m_Ad_Org_ID+","+
						m_AD_PInstance_ID+","+
						"to_timestamp("+(m_DateAcct!=null?"'"+m_DateAcct.toString()+"'":"null")+",'YYYY-MM-DD HH24:MI:SS.FF')," +
						"to_timestamp("+(m_DateAcct_To!=null?"'"+m_DateAcct_To.toString()+"'":"null")+",'YYYY-MM-DD HH24:MI:SS.FF')," +
						"to_timestamp("+(m_DateInvoiced!=null?"'"+m_DateInvoiced.toString()+"'":"null")+",'YYYY-MM-DD HH24:MI:SS.FF')," +
						"to_timestamp("+(m_DateInvoiced_To!=null?"'"+m_DateInvoiced_To.toString()+"'":"null")+",'YYYY-MM-DD HH24:MI:SS.FF'), " +
						"CR.C_BPartner_ID " +
						"from XX_RV_Comprobante_Retencion CR " +
						"Where CR.DocStatus IN ('CO', 'CL') AND CR.IsSotrx = 'N' AND CR.DocBaseType IN ('API', 'APC') AND CR.XX_TipoRetencion = 'IS' ");
						//"CI.DocStatus Not In ('DR') And CI.IsActive='Y' ");
			/*Filtrando Por fecha de documento*/
			/*sql.append((m_DateInvoiced!=null && m_DateInvoiced_To!=null?" And CI.DateInvoiced Between '"+m_DateInvoiced.toString()+"' And '"+m_DateInvoiced_To.toString()+"'":""));
			sql.append((m_DateInvoiced!=null && m_DateInvoiced_To==null?" And CI.DateInvoiced >='"+m_DateInvoiced.toString()+"'":""));
			sql.append((m_DateInvoiced==null && m_DateInvoiced_To!=null?" And CI.DateInvoiced <='"+m_DateInvoiced_To.toString()+"'":""));*/
			/*Filtrando Por fecha de Contabilizacion del documento*/
			sql.append((m_DateAcct!=null && m_DateAcct_To!=null?" And CR.DateAcct Between to_timestamp('"+m_DateAcct.toString()+"','YYYY-MM-DD HH24:MI:SS.FF') And to_timestamp('"+m_DateAcct_To.toString()+"','YYYY-MM-DD  HH24:MI:SS.FF')":""));
			sql.append((m_DateAcct!=null && m_DateAcct_To==null?" And CR.DateAcct >= to_timestamp('"+m_DateAcct.toString()+"','YYYY-MM-DD HH24:MI:SS.FF')":""));
			sql.append((m_DateAcct==null && m_DateAcct_To!=null?" And CR.DateAcct <= to_timestamp('"+m_DateAcct_To.toString()+"','YYYY-MM-DD HH24:MI:SS.FF')":""));
			/*Filtrando Por socio de negocio*/
			sql.append((m_C_BPartner_ID!=null ?" And CR.C_BPartner_ID="+m_C_BPartner_ID:""));
			sql.append(" Group by CR.C_BPartner_ID " +
						" Having Sum(montoretenido)<>0 ");
	}
	
	private Integer m_AD_PInstance_ID;
	private Integer m_Ad_Client_ID;
	private Integer m_Ad_Org_ID;
	private Integer m_C_BPartner_ID;
	private Timestamp m_DateInvoiced;
	private Timestamp m_DateInvoiced_To;
	private Timestamp m_DateAcct;
	private Timestamp m_DateAcct_To;
}
