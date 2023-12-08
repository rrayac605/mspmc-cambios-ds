package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import javax.validation.Valid;

import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;

public interface MovimientosRepository {

	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position, @Valid String numFolioMovtoOriginal);

	Object updateMovimiento(DatosModificadosDTO input);

	ResponseDTO<List<MovimientosOutputDTO>> getMovements(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList,
			@Valid String numNss, @Valid String refRegistroPatronal, @Valid String cveSituacionRegistro,
			@Valid String fromDay, @Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc,
			@Valid Integer cveClase, @Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta,
			@Valid Long page, @Valid Long skipOffset, @Valid Long size, @Valid List<AggregationOperation> customAggregarionList,
			@Valid AggregationOperation sortOperation, @Valid Boolean isOperative, @Valid Boolean isApprover, 
			@Valid Boolean isCasuistry, @Valid Long posicionMovimiento);

	ResponseDTO<Void> getCount(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid MatchOperation customCriteria,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry);

	ResponseDTO<List<DetalleRegistroDTO>> getMovements(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid Long page,
			@Valid Long skipOffset, Boolean paginated, @Valid Long size, @Valid List<AggregationOperation> customAggregarionList,
			@Valid AggregationOperation sortOperation, @Valid Boolean isOperative, @Valid Boolean isApprover, 
			@Valid Boolean isCasuistry, @Valid Long posicionMovimiento);

	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId);
	
}

