package mx.gob.imss.cit.pmc.cambios.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mx.gob.imss.cit.mspmccommons.dto.AseguradoDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.IncapacidadDTO;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.mspmccommons.utils.ListUtils;
import mx.gob.imss.cit.pmc.cambios.repository.PmcDetalleRegistroRepository;
import mx.gob.imss.cit.pmc.cambios.service.PmcValidaSusceptibles;

@Component
public class PmcValidaSusceptiblesImpl implements PmcValidaSusceptibles {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private PmcDetalleRegistroRepository detalleRegistroRepository;

	@Override
	public Map<Integer, List<String>> validarSusceptibles(DetalleRegistroDTO registroDTO) throws BusinessException {
		Integer[] ciclo = DateUtils.obetenerCliclo();
		Boolean porcentajeMayorCero = null;
		List<String> registrosCorrectos = new ArrayList<>();
		List<String> registrosCoincidentes = new ArrayList<>();
		try {
			List<DetalleRegistroDTO> registro = new ArrayList<>();
			List<DetalleRegistroDTO> registroSus = detalleRegistroRepository.existeSusceptibleNss(registroDTO);
			List<CambioDTO> registroSusCambios = detalleRegistroRepository.existeSusceptibleNssCambios(registroDTO);
			if (ListUtils.isNotEmpty(registroSusCambios)) {
				registro.addAll(detalleRegistroDTOtoRegistroDTO(registroSusCambios));
			}
			if (ListUtils.isNotEmpty(registroSus)) {
				registro.addAll(registroSus);
			}
			int existenCicloActual = 0;
			for (DetalleRegistroDTO detalleRegistroDTO : registro) {
				registrosCoincidentes.add(detalleRegistroDTO.getObjectIdArchivoDetalle().toString());
				if (Arrays.asList(ciclo)
						.contains(Integer.valueOf(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()))
						&& ((detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() != null
								&& detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad().intValue() > 0)
								|| (detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia() != null
										&& detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia().equals("4")))) {
					porcentajeMayorCero = true;
				}
				else if(Arrays.asList(ciclo)
						.contains(Integer.valueOf(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()))) {
					existenCicloActual++;
				}
			}
			if (existenCicloActual > 0 && porcentajeMayorCero == null) {
				porcentajeMayorCero = false;
			}
			fillRegisterList(registro, ciclo, registrosCoincidentes, registrosCorrectos, porcentajeMayorCero);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
		logger.debug("Susceptibles de ajuste: {}", registrosCoincidentes.size());

		if (registrosCoincidentes.size() <= 1) {
			registrosCorrectos.addAll(registrosCoincidentes);
			registrosCoincidentes = new ArrayList<>();
		}
		Map<Integer, List<String>> registrosFinales = new HashMap<>();
		registrosFinales.put(1, registrosCoincidentes);
		registrosFinales.put(2, registrosCorrectos);
		return registrosFinales;
	}
	
	@Override
	public List<DetalleRegistroDTO> obtenerSusceptiblesSinCambios(DetalleRegistroDTO registroDTO) throws BusinessException {
		List<DetalleRegistroDTO> registro = new ArrayList<>();
		try {
			List<DetalleRegistroDTO> registroSusAux = detalleRegistroRepository.existeSusceptibleNssByEstado(registroDTO);
			List<DetalleRegistroDTO> registroSus = new ArrayList<DetalleRegistroDTO>();
			for (DetalleRegistroDTO detalleRegistroDTO : registroSusAux) {
				boolean confirmado = detalleRegistroDTO.getConfirmarSinCambios() == null ? false : detalleRegistroDTO.getConfirmarSinCambios();
				if(!confirmado) {
					registroSus.add(detalleRegistroDTO);					
				}
			}
			List<CambioDTO> registroSusCambios = detalleRegistroRepository.existeSusceptibleNssCambiosByEstado(registroDTO);
			if (ListUtils.isNotEmpty(registroSusCambios)) {
				registro.addAll(detalleRegistroDTOtoRegistroDTO(registroSusCambios));
			}
			if (ListUtils.isNotEmpty(registroSus)) {
				registro.addAll(registroSus);
			}
		} catch (Exception e) {
			throw new BusinessException(e);
		}
		logger.debug("Susceptibles de ajuste: {}", registro.size());

		
		return registro;
	}

	@Override
	public Map<Integer, List<String>> validarSusceptiblesSinCambios(DetalleRegistroDTO registroDTO) throws BusinessException {
		Integer[] ciclo = DateUtils.obetenerCliclo();
		Boolean porcentajeMayorCero = null;
		List<String> registrosCorrectos = new ArrayList<>();
		List<String> registrosCoincidentes = new ArrayList<>();
		try {
			List<DetalleRegistroDTO> registro = new ArrayList<>();
			List<DetalleRegistroDTO> registroSusAux = detalleRegistroRepository.existeSusceptibleNss(registroDTO);
			List<DetalleRegistroDTO> registroSus = new ArrayList<DetalleRegistroDTO>();
			List<CambioDTO> registroSusCambios = new ArrayList<CambioDTO>();
			for (DetalleRegistroDTO detalleRegistroDTO : registroSusAux) {
				boolean confirmado = detalleRegistroDTO.getConfirmarSinCambios() == null ? false : detalleRegistroDTO.getConfirmarSinCambios();
				if(!confirmado) {
					registroSus.add(detalleRegistroDTO);					
				}
			}
			List<CambioDTO> registroSusCambiosAux = detalleRegistroRepository.existeSusceptibleNssCambios(registroDTO);
			for (CambioDTO detalleRegistroDTO : registroSusCambiosAux) {
				boolean confirmado = detalleRegistroDTO.getConfirmarSinCambios() == null ? false : detalleRegistroDTO.getConfirmarSinCambios();
				if(!confirmado) {
					registroSusCambios.add(detalleRegistroDTO);					
				}
			}
			if (ListUtils.isNotEmpty(registroSusCambios)) {
				registro.addAll(detalleRegistroDTOtoRegistroDTO(registroSusCambios));
			}
			if (ListUtils.isNotEmpty(registroSus)) {
				registro.addAll(registroSus);
			}
			int existenCicloActual = 0;
			for (DetalleRegistroDTO detalleRegistroDTO : registro) {
				registrosCoincidentes.add(detalleRegistroDTO.getObjectIdArchivoDetalle().toString());
				if (Arrays.asList(ciclo)
						.contains(Integer.valueOf(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()))
						&& ((detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() != null
								&& detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad().intValue() > 0)
								|| (detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia() != null
										&& detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia().equals("4")))) {
					porcentajeMayorCero = true;
				}
				else if(Arrays.asList(ciclo)
						.contains(Integer.valueOf(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()))) {
					existenCicloActual++;
				}
			}
			if (existenCicloActual > 0 && porcentajeMayorCero == null) {
				porcentajeMayorCero = false;
			}
			fillRegisterList(registro, ciclo, registrosCoincidentes, registrosCorrectos, porcentajeMayorCero);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
		logger.debug("Susceptibles de ajuste: {}", registrosCoincidentes.size());

		if (registrosCoincidentes.size() <= 1) {
			registrosCorrectos.addAll(registrosCoincidentes);
			registrosCoincidentes = new ArrayList<>();
		}
		Map<Integer, List<String>> registrosFinales = new HashMap<>();
		registrosFinales.put(1, registrosCoincidentes);
		registrosFinales.put(2, registrosCorrectos);
		return registrosFinales;
	}
	
	private void fillRegisterList(List<DetalleRegistroDTO> registro, Integer[] ciclo, List<String> registrosCoincidentes,
								  List<String> registrosCorrectos, Boolean porcentajeMayorCero) {
		for (DetalleRegistroDTO detalleRegistroDTO : registro) {
			logger.debug(
					"Datos del registro -> nss: {}, ciclo: {}, cicloActual: {}, porcentaje: {}, consecuencia: {}, subsidiados: {} ",
					detalleRegistroDTO.getAseguradoDTO().getNumNss(),
					detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual(), ciclo,
					detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad(),
					detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia(),
					detalleRegistroDTO.getIncapacidadDTO().getNumDiasSubsidiados());
			if(detalleRegistroDTO.getConfirmarSinCambios() != null && detalleRegistroDTO.getConfirmarSinCambios().equals(Boolean.TRUE)) {
				removeRegister(detalleRegistroDTO, registrosCoincidentes, registrosCorrectos);
			}
			else if (Integer.parseInt(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()) < ciclo[0]
					&& (detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() == null
					|| (detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() != null
					&& detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad().intValue() <= 0)
					&& (detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia() != null
					&& !detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia().equals("4")))) {
				removeRegister(detalleRegistroDTO, registrosCoincidentes, registrosCorrectos);
			} else if (Integer.parseInt(detalleRegistroDTO.getAseguradoDTO().getNumCicloAnual()) < ciclo[0]
					&& (detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() == null
					|| (detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad() != null
					&& detalleRegistroDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad().intValue() > 0)
					|| (detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia() != null
					&& detalleRegistroDTO.getIncapacidadDTO().getCveConsecuencia().equals("4")))
					&& (porcentajeMayorCero != null && !porcentajeMayorCero)) {
				removeRegister(detalleRegistroDTO, registrosCoincidentes, registrosCorrectos);
			}
		}
	}

	private void removeRegister(DetalleRegistroDTO detalleRegistroDTO, List<String> registrosCoincidentes,
								List<String> registrosCorrectos) {
		for (String susceptible : registrosCoincidentes) {
			if (susceptible.equals(detalleRegistroDTO.getObjectIdArchivoDetalle().toString())) {
				logger.debug("removiendo registro {}", susceptible);
				registrosCoincidentes.remove(susceptible);
				registrosCorrectos.add(susceptible);
				break;
			}
		}
	}

	public static List<DetalleRegistroDTO> detalleRegistroDTOtoRegistroDTO(List<CambioDTO> registroSusCambios) {
		List<DetalleRegistroDTO> listaDetalles = new ArrayList<>();
		for (CambioDTO cambioDTO : registroSusCambios) {
			DetalleRegistroDTO detalle = new DetalleRegistroDTO();
			detalle.setAseguradoDTO(new AseguradoDTO());
			detalle.setIncapacidadDTO(new IncapacidadDTO());
			detalle.setObjectIdArchivoDetalle(new ObjectId(cambioDTO.getObjectIdCambio()));
			detalle.setCveOrigenArchivo(cambioDTO.getCveOrigenArchivo());
			detalle.getAseguradoDTO().setNumNss(cambioDTO.getNumNss());
			detalle.getAseguradoDTO().setNumCicloAnual(cambioDTO.getNumCicloAnual());
			detalle.getAseguradoDTO().setCveEstadoRegistro(cambioDTO.getCveEstadoRegistro());
			detalle.getIncapacidadDTO().setCveConsecuencia(String.valueOf(cambioDTO.getCveConsecuencia()));
			detalle.getIncapacidadDTO().setNumDiasSubsidiados(cambioDTO.getNumDiasSubsidiados());
			detalle.getIncapacidadDTO().setPorPorcentajeIncapacidad(cambioDTO.getPorcentajeIncapacidad());
			detalle.setConfirmarSinCambios(cambioDTO.getConfirmarSinCambios());
			listaDetalles.add(detalle);
		}
		return listaDetalles;
	}

}
