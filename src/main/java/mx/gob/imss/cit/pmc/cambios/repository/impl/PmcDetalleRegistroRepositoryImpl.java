package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.ArchivoDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.enums.CamposAseguradoEnum;
import mx.gob.imss.cit.mspmccommons.enums.CamposIncapacidadEnum;
import mx.gob.imss.cit.mspmccommons.enums.CamposPatronEnum;
import mx.gob.imss.cit.mspmccommons.enums.EstadoRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.SituacionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.utils.CustomAggregationOperation;
import mx.gob.imss.cit.pmc.cambios.repository.PmcDetalleRegistroRepository;

@Repository
public class PmcDetalleRegistroRepositoryImpl implements PmcDetalleRegistroRepository {

	private static final String MCT_ARCHIVO = "MCT_ARCHIVO";
	private static final String ID_ARCHIVO = "identificadorArchivo";
	private static final String ARCHIVO_DTO = "archivoDTO";

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	public boolean existeRegistro(DetalleRegistroDTO registroDTO) {

		Query query = new Query(Criteria.where(CamposPatronEnum.RP.getNombreCampo())
				.is(registroDTO.getPatronDTO().getRefRegistroPatronal()).and(CamposAseguradoEnum.NSS.getNombreCampo())
				.is(registroDTO.getAseguradoDTO().getNumNss()).and(CamposIncapacidadEnum.TIPO_RIESGO.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getCveTipoRiesgo())
				.and(CamposIncapacidadEnum.CONSECUENCIA.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getCveConsecuencia())
				.and(CamposIncapacidadEnum.FECHA_INICIO.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getFecInicio())
				.and(CamposIncapacidadEnum.FECHA_FIN.getNombreCampo()).is(registroDTO.getIncapacidadDTO().getFecFin())
				.and(CamposIncapacidadEnum.DIAS_SUBSIDIADOS.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getNumDiasSubsidiados())
				.and(CamposIncapacidadEnum.PORCENTAJE_INCAPACIDAD.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad()));

		return mongoOperations.exists(query, DetalleRegistroDTO.class);
	}

	@Override
	public List<DetalleRegistroDTO> existeSusceptible(DetalleRegistroDTO registroDTO) {

		Query query = new Query(Criteria.where(CamposPatronEnum.RP.getNombreCampo())
				.is(registroDTO.getPatronDTO().getRefRegistroPatronal()).and(CamposAseguradoEnum.NSS.getNombreCampo())
				.is(registroDTO.getAseguradoDTO().getNumNss()).and(CamposAseguradoEnum.CURP.getNombreCampo())
				.is(registroDTO.getAseguradoDTO().getRefCurp()).and(CamposIncapacidadEnum.TIPO_RIESGO.getNombreCampo())
				.is(registroDTO.getIncapacidadDTO().getCveTipoRiesgo()));

		return mongoOperations.find(query, DetalleRegistroDTO.class);
	}

	@Override
	public List<DetalleRegistroDTO> existeSusceptibleNss(DetalleRegistroDTO registroDTO) {

		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, ID_ARCHIVO, "_id", ARCHIVO_DTO);
		String jsonOpperation = "{ $project: {"
				+ " '_id':1,  'identificadorArchivo':1, 'confirmarSinCambios': 1, 'aseguradoDTO.numNss': 1, 'aseguradoDTO.numCicloAnual': 1,'aseguradoDTO.cveEstadoRegistro': 1,"
				+ " 'incapacidadDTO.cveConsecuencia': 1, 'incapacidadDTO.numDiasSubsidiados': 1,"
				+ " 'incapacidadDTO.porPorcentajeIncapacidad': 1, 'aseguradoDTO.cveOrigenArchivo': 1, 'aseguradoDTO.cveOrigenArchivo': 1}}";
		CustomAggregationOperation projection = new CustomAggregationOperation(jsonOpperation);
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class,
				Aggregation.match(Criteria.where(CamposAseguradoEnum.NSS.getNombreCampo())
						.is(registroDTO.getAseguradoDTO().getNumNss())),
				Aggregation.match(Criteria.where(CamposAseguradoEnum.FECHA_BAJA.getNombreCampo()).is(null)), lookup,
				projection);

		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);

		return aggregationResults.getMappedResults();
	}

	@Override
	public List<CambioDTO> existeSusceptibleNssCambios(DetalleRegistroDTO registroDTO) {

		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, ID_ARCHIVO, "_id", ARCHIVO_DTO);
		String jsonOpperation = "{ $project: {"
				+ " '_id':1,  'identificadorArchivo':1,  'numNss': 1,  'numCicloAnual': 1,'cveEstadoRegistro': 1,"
				+ " 'cveConsecuencia': 1," + "  'numDiasSubsidiados': 1, 'confirmarSinCambios': 1,"
				+ " 'porcentajeIncapacidad': 1, 'cveOrigenArchivo': 1}}";
		CustomAggregationOperation projection = new CustomAggregationOperation(jsonOpperation);
		TypedAggregation<CambioDTO> aggregation = Aggregation.newAggregation(CambioDTO.class,
				Aggregation.match(Criteria.where("numNss").is(registroDTO.getAseguradoDTO().getNumNss())),
				Aggregation.match(Criteria.where("cveEstadoRegistro")
						.nin(EstadoRegistroEnum.BAJA.getCveEstadoRegistro(),
								EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro())),
				Aggregation.match(Criteria.where(CamposAseguradoEnum.FECHA_BAJA_CAMBIO.getNombreCampo()).is(null)),
				Aggregation.match(Criteria.where("cveSituacionRegistro").is(SituacionRegistroEnum.APROBADO.getClave())),
				lookup, projection);

		AggregationResults<CambioDTO> aggregationResults = mongoOperations.aggregate(aggregation, CambioDTO.class);

		return aggregationResults.getMappedResults();
	}
	
	@Override
	public List<DetalleRegistroDTO> existeSusceptibleNssByEstado(DetalleRegistroDTO registroDTO) {

		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, ID_ARCHIVO, "_id", ARCHIVO_DTO);
		String jsonOpperation = "{ $project: {"
				+ " '_id':1,  'identificadorArchivo':1, 'confirmarSinCambios': 1, 'aseguradoDTO.numNss': 1, 'aseguradoDTO.numCicloAnual': 1,'aseguradoDTO.cveEstadoRegistro': 1,"
				+ " 'incapacidadDTO.cveConsecuencia': 1, 'incapacidadDTO.numDiasSubsidiados': 1,"
				+ " 'incapacidadDTO.porPorcentajeIncapacidad': 1, 'aseguradoDTO.cveOrigenArchivo': 1, 'aseguradoDTO.cveOrigenArchivo': 1}}";
		CustomAggregationOperation projection = new CustomAggregationOperation(jsonOpperation);
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class,
				Aggregation.match(Criteria.where(CamposAseguradoEnum.NSS.getNombreCampo())
						.is(registroDTO.getAseguradoDTO().getNumNss())),		
				Aggregation.match(Criteria.where(CamposAseguradoEnum.ESTADO_REGISTRO.getNombreCampo())
								.in(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro(),
									EstadoRegistroEnum.SUSCEPTIBLE_OTRAS.getCveEstadoRegistro())),
				Aggregation.match(Criteria.where(CamposAseguradoEnum.FECHA_BAJA.getNombreCampo()).is(null)), lookup,
				projection);

		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);

		return aggregationResults.getMappedResults();
	}

	@Override
	public List<CambioDTO> existeSusceptibleNssCambiosByEstado(DetalleRegistroDTO registroDTO) {

		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, ID_ARCHIVO, "_id", ARCHIVO_DTO);
		String jsonOpperation = "{ $project: {"
				+ " '_id':1,  'identificadorArchivo':1,  'numNss': 1,  'numCicloAnual': 1,'cveEstadoRegistro': 1,"
				+ " 'cveConsecuencia': 1," + "  'numDiasSubsidiados': 1, 'confirmarSinCambios': 1,"
				+ " 'porcentajeIncapacidad': 1, 'cveOrigenArchivo': 1}}";
		CustomAggregationOperation projection = new CustomAggregationOperation(jsonOpperation);
		TypedAggregation<CambioDTO> aggregation = Aggregation.newAggregation(CambioDTO.class,
				Aggregation.match(Criteria.where("numNss").is(registroDTO.getAseguradoDTO().getNumNss())),
				Aggregation.match(Criteria.where("cveEstadoRegistro")
						.in(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro(),
							EstadoRegistroEnum.SUSCEPTIBLE_OTRAS.getCveEstadoRegistro())),
				Aggregation
						.match(Criteria.where("cveEstadoRegistro").nin(EstadoRegistroEnum.BAJA.getCveEstadoRegistro(),
								EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro())),
				Aggregation.match(Criteria.where(CamposAseguradoEnum.FECHA_BAJA_CAMBIO.getNombreCampo()).is(null)),
				lookup, projection);

		AggregationResults<CambioDTO> aggregationResults = mongoOperations.aggregate(aggregation, CambioDTO.class);

		return aggregationResults.getMappedResults();
	}

	@Override
	public ArchivoDTO getArchivoByIdMovimiento(ObjectId objectIdMovimiento) {
		MatchOperation match = Aggregation.match(Criteria.where("objectIdArchivoDetalle").is(objectIdMovimiento));
		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, ID_ARCHIVO, "_id", ARCHIVO_DTO);
		UnwindOperation unwind = Aggregation.unwind(ARCHIVO_DTO);
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class, match,
				lookup, unwind);
		AggregationResults<DetalleRegistroDTO> aggregationResult = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);
		DetalleRegistroDTO detalle = aggregationResult.getUniqueMappedResult();
		return detalle != null ? detalle.getArchivoDTO() : null;
	}

}