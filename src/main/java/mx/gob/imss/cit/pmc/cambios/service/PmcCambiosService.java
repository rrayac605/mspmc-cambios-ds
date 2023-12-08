package mx.gob.imss.cit.pmc.cambios.service;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;

import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.MovimientoRequest;
import mx.gob.imss.cit.mspmccommons.dto.ReporteCasuisticaInputDTO;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;

public interface PmcCambiosService {

	DetalleRegistroResponse guardarCambio(@RequestBody DetalleRegistroResponse input) throws BusinessException;
	
	DetalleRegistroDTO guardarCambio(@RequestBody DetalleRegistroDTO input) throws BusinessException;

	void eliminarMovimiento(@RequestBody DetalleRegistroResponse input) throws BusinessException;
	
	void eliminarMovimiento(@RequestBody DetalleRegistroDTO input) throws BusinessException;

	Object findCambiosMovimientos(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo,
			@Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro, @Valid String fromMonth,
			@Valid String fromYear, @Valid String toMonth, @Valid String toYear, @Valid List<Integer> cveEstadoRegistroList,
			@Valid String numNss, @Valid String refRegistroPatronal, @Valid String cveSituacionRegistro,
			@Valid String fromDay, @Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid Long page, @Valid Long totalElements, @Valid String origenAlta,
			@Valid Long totalElementsMovement, @Valid Long changesMinorThanMovements, @Valid Boolean isOperative,
			@Valid Boolean isApprover, Boolean isCasuistry) throws BusinessException;

	List<CambioDTO> findMovimientosReporte(ReporteCasuisticaInputDTO input) throws BusinessException;

	DetalleRegistroResponse getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal) throws BusinessException;

	Object updateCambio(MovimientoRequest input) throws BusinessException;

	Boolean guardarSinCambios(MovimientoRequest input) throws BusinessException;
	
	Object aprobarSinCambios(MovimientoRequest input) throws BusinessException;
	
	DetalleRegistroResponse guardarCambioNuevo(DetalleRegistroResponse input) throws BusinessException;
	
	DetalleRegistroResponse guardarCambioNuevoSus(DetalleRegistroResponse input,String pantalla) throws BusinessException;
	
	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal, @Valid Boolean isChange) throws BusinessException;
	
}
