package mx.gob.imss.cit.pmc.cambios.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mx.gob.imss.cit.mspmccommons.dto.ConsecuenciaDTO;
import mx.gob.imss.cit.mspmccommons.dto.LaudoDTO;
import mx.gob.imss.cit.mspmccommons.dto.ModificacionPatronalDTO;
import mx.gob.imss.cit.mspmccommons.dto.TipoRiesgoDTO;
import mx.gob.imss.cit.pmc.cambios.repository.ConsecuenciaRepository;
import mx.gob.imss.cit.pmc.cambios.repository.LaudoRepository;
import mx.gob.imss.cit.pmc.cambios.repository.ModificacionPatronalRepository;
import mx.gob.imss.cit.pmc.cambios.repository.TipoRiesgoRepository;
import mx.gob.imss.cit.pmc.cambios.service.PmcCatalogosService;

@Component
public class PmcCatalogoServiceImpl implements PmcCatalogosService {

	private static final Logger logger = LoggerFactory.getLogger(PmcCatalogoServiceImpl.class);

	@Autowired
	private LaudoRepository laudoRepository;

	@Autowired
	private ConsecuenciaRepository consecuenciaRepository;

	@Autowired
	private TipoRiesgoRepository tipoRiesgoRepository;

	@Autowired
	private ModificacionPatronalRepository  modificacionPatronalRepository;
	
	@Override
	public TipoRiesgoDTO obtenerTipoRiesgo(String cveTipoRiesgo) {

		TipoRiesgoDTO tipoRiesgoDTO = null;
		try {
			tipoRiesgoDTO = tipoRiesgoRepository.findOneByCve(cveTipoRiesgo).get();
		} catch (Exception e) {
			logger.error("No existe información para el tipo de riesgo: {} {}", cveTipoRiesgo, e);
		}
		return tipoRiesgoDTO;
	}

	@Override
	public ConsecuenciaDTO obtenerConsecuencia(String cveConsecuencia) {
		ConsecuenciaDTO consecuenciaDTO = null;
		try {
			consecuenciaDTO = consecuenciaRepository.findOneByCve(cveConsecuencia).get();
		} catch (Exception e) {
			logger.error("No existe información para la consecuencia: {} {}", cveConsecuencia, e);
		}
		return consecuenciaDTO;
	}

	@Override
	public LaudoDTO obtenerLaudo(String cveLaudo) {
		LaudoDTO laudoDTO = null;
		try {
			laudoDTO = laudoRepository.findOneByCve(cveLaudo).get();
		} catch (Exception e) {
			logger.error("No existe información para el laudo: {} {}", cveLaudo, e);
		}
		return laudoDTO;
	}

	@Override
	public ModificacionPatronalDTO obtenerModificacionPatronal(String cveModificacionPatronal) {
		ModificacionPatronalDTO modificacionPatronalDTO = null;
		try {
			modificacionPatronalDTO = modificacionPatronalRepository.findOneByCve(cveModificacionPatronal).get();
		} catch (Exception e) {
			logger.error("No existe información para modificacion Patronal: {} {}", cveModificacionPatronal, e);
		}
		return modificacionPatronalDTO;
	}
	
	

}
