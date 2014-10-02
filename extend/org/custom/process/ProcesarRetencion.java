/**
 * @Finalidad Crear lineas de retencion en las facturas hechas a Proveedores
 * @author Yamel Senih
 * @Fecha 22/02/2011
 */
package org.custom.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.compiere.apps.ADialog;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *  @author Yamel Senih 22/02/2011, 13:55:35
 */
public class ProcesarRetencion extends SvrProcess {

	/* (non-Javadoc)
	 * @see org.compiere.process.SvrProcess#doIt()
	 */
	@Override
	protected String doIt() throws Exception {
		MInvoice factura = new MInvoice(Env.getCtx(), idFactura, null);
		MInvoiceLine [] lineasFac = factura.getLines();
		MInvoiceLine lineaRetencionISLR = null;
		MInvoiceLine lineaRetencionIVA = null;
		BigDecimal montoLineaISLR = Env.ZERO;
		StringBuffer resultadoProceso = new StringBuffer();
		/*
		 * Se verifica si existen líneas en la Factura"
		 */
		if(lineasFac.length != 0){
			/*
			 * Se recorren las líneas de la factura, si existe la linea 9900
			 * y la descripcion es igual a "Retencion ISLR", entonces se almacena
			 * esa linea en un objeto llamado lineaRetencionISLR y se modifica,
			 * en caso de no encontrar la linea se crea una nueva;
			 * Ésto ocurre tambien con las lineas de IVA
			 */
			for(MInvoiceLine linea : lineasFac){
				if((linea.getLine() == NRO_LINEA_ISLR) 
					&& (linea.get_ValueAsString("XX_TipoRetencion") != null && linea.get_ValueAsString("XX_TipoRetencion").equals(TIPO_ISLR))){
					lineaRetencionISLR = linea;
					montoLineaISLR = lineaRetencionISLR.getLineNetAmt();
				} else if((linea.getLine() == NRO_LINEA_IVA) 
					&& (linea.get_ValueAsString("XX_TipoRetencion") != null && linea.get_ValueAsString("XX_TipoRetencion").equals(TIPO_IVA))) {
					lineaRetencionIVA = linea;
				}
			}
			/*
			 * Si la línea no existe se crea en ambos casos
			 * */
			if(lineaRetencionISLR == null){
				lineaRetencionISLR = new MInvoiceLine(factura);
				lineaRetencionISLR.setC_Activity_ID(factura.getC_Activity_ID());
			}
			if(lineaRetencionIVA == null){
				lineaRetencionIVA = new MInvoiceLine(factura);
				lineaRetencionIVA.setC_Activity_ID(factura.getC_Activity_ID());
			}
			
			/*
			 * Se procesa la línea de ISLR 
			 * */
			resultadoProceso.append("ISLR[");
			//if(factura.get_ValueAsInt("XX_ConceptoRetencion_ID") != 0){
			if(factura.get_ValueAsInt("XX_CONCEPTORETENCION_ID") != 0){
				resultadoProceso.append(procesaLineaISLR(lineaRetencionISLR, montoLineaISLR));
			}else {
				lineaRetencionISLR.delete(true);
				resultadoProceso.append("Debe Seleccionar el Concepto de Retención");
			}
			resultadoProceso.append("] IVA[");
			/*Se procesa la línea de IVA
			 * */
			resultadoProceso.append(procesaLineaIVA(lineaRetencionIVA));
			resultadoProceso.append("]");
			return resultadoProceso.toString();
		}
		ADialog.error(0, null, "Error al verificar líneas de Factura:", "La Factura no tiene Líneas");
		return "La Factura no tiene Líneas";
	}

	/**
	 * Realiza el calculo de Retencion de I.S.L.R. y crea la linea
	 * @author Yamel Senih 24/02/2011, 20:09:16
	 * @param lineaRetencion
	 * @param montoLineaRestar
	 * @return
	 * @throws SQLException
	 * @return String
	 */
	private String procesaLineaISLR(MInvoiceLine lineaRetencion, BigDecimal montoLineaRestar) throws SQLException{
		BigDecimal netoFactura = Env.ZERO;
		BigDecimal montoRetencion = Env.ZERO;
		BigDecimal alicuota = Env.ZERO;
		BigDecimal minimo = Env.ZERO;
		BigDecimal sustraendo = Env.ZERO;
		int idImpuesto = 0;
		int idCargo = 0;
		PreparedStatement pstmt = null;
		String sql = new String("SELECT (CASE WHEN cp.XX_Posee_Carta_Retencion = 'Y' " +
				"THEN (SUM(CASE WHEN XX_Retiene = 'Y' THEN lfac.LineNetAmt ELSE 0 END)) " +
				"ELSE SUM(lfac.LineNetAmt) END - ?) netoFactura, " +
				"ret.XX_Alicuota alicuota, ret.XX_Minimo minimo, " +
				"ret.XX_Sustraendo sustraendo, ret.C_Tax_ID impuesto, ret.C_Charge_ID cargo " +
				"FROM C_Invoice fac INNER JOIN C_InvoiceLine lfac ON(lfac.C_Invoice_ID = fac.C_Invoice_ID) " +
				"INNER JOIN C_BPartner cp ON(cp.C_BPartner_ID = fac.C_BPartner_ID) " +
				"INNER JOIN XX_Retencion ret ON(ret.XX_TipoPersona_ID = cp.XX_TipoPersona_ID) " +
				"INNER JOIN XX_ConceptoRetencion cret ON(cret.XX_ConceptoRetencion_ID = ret.XX_ConceptoRetencion_ID) " +
				"WHERE fac.C_Invoice_ID = ? " +
				"AND cret.XX_ConceptoRetencion_ID = fac.XX_ConceptoRetencion_ID AND XX_Retiene = 'Y' " +
				"GROUP BY fac.C_Invoice_ID, ret.XX_Alicuota, " +
		"ret.XX_Minimo, ret.XX_Sustraendo, ret.C_Tax_ID, ret.C_Charge_ID, cp.XX_Posee_Carta_Retencion");
		pstmt = DB.prepareStatement(sql, null);
		pstmt.setBigDecimal(1, montoLineaRestar);
		pstmt.setInt(2, idFactura);
		ResultSet res = pstmt.executeQuery();
		if(res != null){
			while(res.next()){
				netoFactura = res.getBigDecimal("netoFactura");
				alicuota = res.getBigDecimal("alicuota");
				minimo = res.getBigDecimal("minimo");
				sustraendo = res.getBigDecimal("sustraendo");
				idImpuesto = res.getInt("impuesto");
				idCargo = res.getInt("cargo");
			}
				
			if(netoFactura.compareTo(minimo) > 0){
				alicuota = alicuota.divide(Env.ONEHUNDRED);
				montoRetencion = netoFactura.multiply(alicuota);
				montoRetencion = montoRetencion.subtract(sustraendo);
				montoRetencion = (montoRetencion.compareTo(Env.ZERO) < 0? Env.ZERO: montoRetencion);
					
				/*
				 * Se establecen los valores para la linea de Retención de I.S.L.R
				 */
					
				lineaRetencion.set_Value("XX_TipoRetencion", TIPO_ISLR);
				lineaRetencion.setQty(Env.ONE);
				lineaRetencion.setC_Tax_ID(idImpuesto);
				lineaRetencion.setC_Charge_ID(idCargo);
				lineaRetencion.setLine(NRO_LINEA_ISLR);
				lineaRetencion.setC_UOM_ID(NRO_UOM);
				lineaRetencion.setPrice(montoRetencion.negate());
				lineaRetencion.setLineNetAmt(montoRetencion.negate());
				lineaRetencion.setLineTotalAmt(montoRetencion.negate());
				if(lineaRetencion.save()){
					log.info(lineaRetencion.toString());
					return "\"ID de Linea\" = " + lineaRetencion.getC_InvoiceLine_ID() + 
					" \"Monto Neto\" =" + lineaRetencion.getLineNetAmt().doubleValue() + 
					" \"Monto Total\" = " + lineaRetencion.getLineTotalAmt().doubleValue();
				}
			} else {
				/*Modificado el Dia 24/10/2011 Por Carlos Parada a petición de María Salazar Para que se genere la linea de retencion aun cuando la base de la factura no de el minimo*/
				//lineaRetencion.delete(true);
				//DB.close(res, pstmt);
				//return "El monto neto no cumple con el mínimo de la Retención";
				lineaRetencion.set_Value("XX_TipoRetencion", TIPO_ISLR);
				lineaRetencion.setQty(Env.ONE);
				lineaRetencion.setC_Tax_ID(idImpuesto);
				lineaRetencion.setC_Charge_ID(idCargo);
				lineaRetencion.setLine(NRO_LINEA_ISLR);
				lineaRetencion.setC_UOM_ID(NRO_UOM);
				lineaRetencion.setPrice(Env.ZERO);
				lineaRetencion.setLineNetAmt(Env.ZERO);
				lineaRetencion.setLineTotalAmt(Env.ZERO);
				if(lineaRetencion.save()){
					log.info(lineaRetencion.toString());
					return "Se genero la retención en Cero 0";
				}
				
				
			}
		}
		//DB.close(res, pstmt);
		res.close();
		pstmt.close();
		ADialog.error(0, null, "No se pudo Guardar la Línea");
		return "No se pudo Guardar la Línea";
	}
	
	private String procesaLineaIVA(MInvoiceLine lineaRetencion) throws SQLException{
		BigDecimal totalImpuesto = Env.ZERO;
		BigDecimal porcentajeRetener = Env.ZERO;
		BigDecimal montoRetencion = Env.ZERO;
		int idImpuesto = 0;
		int idCargo = 0;
		PreparedStatement pstmt = null;
		String sql = new String("SELECT SUM(iFac.TaxAmt) Total_Impuesto, ret.Monto_Retener Porc_Retener, " +
				"ret.C_Tax_ID Impuesto, ret.C_Charge_ID Cargo " +
				"FROM XX_RV_ImpuestoFactura iFac " +
				"INNER JOIN C_Invoice fac ON(fac.C_Invoice_ID = iFac.C_Invoice_ID) " +
				"INNER JOIN C_BPartner cp ON(cp.C_BPartner_ID = fac.C_BPartner_ID) " +
				"INNER JOIN XX_Retencion_Iva ret ON(ret.XX_Retencion_Iva_ID = cp.XX_Retencion_Iva_ID) " +
				"WHERE fac.C_Invoice_ID = ? " +
				"GROUP BY fac.C_Invoice_ID, ret.Monto_Retener, ret.C_Tax_ID, ret.C_Charge_ID ");
		pstmt = DB.prepareStatement(sql, null);
		pstmt.setInt(1, idFactura);
		ResultSet res = pstmt.executeQuery();
		
		if(res != null){
			while(res.next()){
				
				totalImpuesto = res.getBigDecimal("Total_Impuesto");
				porcentajeRetener = res.getBigDecimal("Porc_Retener");
				idImpuesto = res.getInt("Impuesto");
				idCargo = res.getInt("Cargo");
			}
			if(totalImpuesto.compareTo(Env.ZERO) > 0){
				porcentajeRetener = porcentajeRetener.divide(Env.ONEHUNDRED);
				montoRetencion = totalImpuesto.multiply(porcentajeRetener);
				
				/*
				 * Se establecen los valores para la linea de Retención de I.S.L.R
				 */
			
				lineaRetencion.set_Value("XX_TipoRetencion", TIPO_IVA);
				lineaRetencion.setQty(Env.ONE);
				lineaRetencion.setC_Tax_ID(idImpuesto);
				lineaRetencion.setC_Charge_ID(idCargo);
				lineaRetencion.setLine(NRO_LINEA_IVA);
				lineaRetencion.setC_UOM_ID(NRO_UOM);
				lineaRetencion.setPrice(montoRetencion.negate());
				lineaRetencion.setLineNetAmt(montoRetencion.negate());
				lineaRetencion.setLineTotalAmt(montoRetencion.negate());
				if(lineaRetencion.save()){
					log.info(lineaRetencion.toString());
					return "\"ID de Linea\" = " + lineaRetencion.getC_InvoiceLine_ID() + 
					" \"Monto Neto\" =" + lineaRetencion.getLineNetAmt().doubleValue() + 
					" \"Monto Total\" = " + lineaRetencion.getLineTotalAmt().doubleValue();
				}
			} else {
				lineaRetencion.delete(true);
				//DB.close(res, pstmt);
				res.close();
				pstmt.close();
				return "No exite impuesto sobre la Factura";
			}
		}
		//DB.close(res, pstmt);
		res.close();
		pstmt.close();
		ADialog.error(0, null, "No se pudo Guardar la Línea");
		return "No se pudo Guardar la Línea";
	}
	
	/* (non-Javadoc)
	 * @see org.compiere.process.SvrProcess#prepare()
	 */
	@Override
	protected void prepare() {
		idFactura = getRecord_ID();
	}

	
	private int idFactura;
	private final String TIPO_ISLR = "IS";
	private final String TIPO_IVA = "IV";
	private final int NRO_LINEA_ISLR = 9900;
	private final int NRO_LINEA_IVA = 9910;
	private final int NRO_UOM = 100;
}
