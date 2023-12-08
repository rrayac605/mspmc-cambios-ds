package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import mx.gob.imss.cit.mspmccommons.dto.*;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;

import org.bson.types.ObjectId;

public interface CambiosRepository {

	void save(CambioDTO cambioDTO);

	Object findCambiosMovimientos(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, @Valid String fromDay, @Valid String toDay,
			@Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase, @Valid Integer cveFraccion,
			@Valid Integer cveLaudo, @Valid Long page, @Valid Long totalElements, @Valid String origenAlta,
			@Valid Long totalElementsMovement, @Valid Long changesMinorThanMovements, @Valid Boolean isOperative,
			@Valid Boolean isApprover, @Valid Boolean isCasuistry) throws BusinessException;

	List<CambioDTO> findMovimientosReporte(ReporteCasuisticaInputDTO input) throws BusinessException;

	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal) throws BusinessException;

	CambioDTO updateCambio(MovimientoRequest input) throws BusinessException;

	CambioDTO getCambio(MovimientoRequest input);

	Optional<CambioDTO> findOneById(String objectId);
	
	void updateCambio(CambioDTO cambioDTO);

	void markAsPending(ObjectId objectId, Boolean isPending);

	void updateAlta(DatosModificadosDTO datosModificadosDTO, MovimientoRequest movimientoRequest);
	
	CambioDTO updateSinCambio(MovimientoRequest input) throws BusinessException;

	boolean isPendienteAprobar(ObjectId idObject);
	
	boolean isPendienteAprobarMN(ObjectId idObject);
	
}
