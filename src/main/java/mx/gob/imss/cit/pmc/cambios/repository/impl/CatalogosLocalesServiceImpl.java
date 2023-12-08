package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mx.gob.imss.cit.mspmccommons.dto.CatalogoDTO;
import mx.gob.imss.cit.mspmccommons.dto.ConsecuenciaDTO;
import mx.gob.imss.cit.mspmccommons.dto.LaudoDTO;
import mx.gob.imss.cit.mspmccommons.dto.ModificacionPatronalDTO;
import mx.gob.imss.cit.mspmccommons.dto.TipoRiesgoDTO;
import mx.gob.imss.cit.pmc.cambios.repository.CatalogosLocales;
import mx.gob.imss.cit.pmc.cambios.repository.ConsecuenciaRepository;
import mx.gob.imss.cit.pmc.cambios.repository.LaudoRepository;
import mx.gob.imss.cit.pmc.cambios.repository.ModificacionPatronalRepository;
import mx.gob.imss.cit.pmc.cambios.repository.TipoRiesgoRepository;

@Component
public class CatalogosLocalesServiceImpl implements CatalogosLocales {

	private HashMap<String, CatalogoDTO> consecuencias;

	private HashMap<String, CatalogoDTO> laudos;

	private HashMap<String, CatalogoDTO> tiposRiesgo;
	
	private HashMap<String, CatalogoDTO> modificaciones;


	@Autowired
	private ConsecuenciaRepository consecuenciaRepository;

	@Autowired
	private LaudoRepository laudoRepository;

	@Autowired
	private TipoRiesgoRepository tipoRiesgoRepository;
	
	@Autowired
	private ModificacionPatronalRepository modificacionPatronalRepository;

	@Override
	public CatalogoDTO obtenerConsecuencia(String cveConsencuencia) {
		CatalogoDTO catalogoDTOResultado = new CatalogoDTO();
		if (consecuencias == null) {
			Optional<List<ConsecuenciaDTO>> consecuenciasLista = consecuenciaRepository.findAll();
			consecuencias = new HashMap<>();
			if (consecuenciasLista.isPresent()) {
				for (ConsecuenciaDTO consecuenciaDTO : consecuenciasLista.get()) {
					CatalogoDTO catalogoDTO = new CatalogoDTO();
					catalogoDTO.setCveCatalogo(String.valueOf(consecuenciaDTO.getCveIdConsecuencia()));
					catalogoDTO.setDesCatalogo(consecuenciaDTO.getDesConsecuencia());
					consecuencias.put(String.valueOf(consecuenciaDTO.getCveIdConsecuencia()), catalogoDTO);
				}
			}
		}
		if (consecuencias.get(cveConsencuencia) != null) {
			catalogoDTOResultado = consecuencias.get(cveConsencuencia);
		}
		return catalogoDTOResultado;
	}

	@Override
	public CatalogoDTO obtenerLaudo(String cveLaudo) {
		CatalogoDTO catalogoDTOResultado = new CatalogoDTO();
		if (laudos == null) {
			Optional<List<LaudoDTO>> laudosLista = laudoRepository.findAll();
			laudos = new HashMap<>();
			if (laudosLista.isPresent()) {
				for (LaudoDTO laudoDTO : laudosLista.get()) {
					CatalogoDTO catalogoDTO = new CatalogoDTO();
					catalogoDTO.setCveCatalogo(String.valueOf(laudoDTO.getCveIdLaudo()));
					catalogoDTO.setDesCatalogo(laudoDTO.getDesLaudo());
					laudos.put(String.valueOf(laudoDTO.getCveIdLaudo()), catalogoDTO);
				}
			}
		}
		if (laudos.get(cveLaudo) != null) {
			catalogoDTOResultado = laudos.get(cveLaudo);
		}
		return catalogoDTOResultado;
	}

	@Override
	public CatalogoDTO obtenerTipoRiesgo(String cveTipoRiesgo) {
		CatalogoDTO catalogoDTOResultado = new CatalogoDTO();
		if (tiposRiesgo == null) {
			Optional<List<TipoRiesgoDTO>> tiposRiesgoLista = tipoRiesgoRepository.findAll();
			tiposRiesgo = new HashMap<>();
			if (tiposRiesgoLista.isPresent()) {
				for (TipoRiesgoDTO tipoRiesgoDTO : tiposRiesgoLista.get()) {
					CatalogoDTO catalogoDTO = new CatalogoDTO();
					catalogoDTO.setCveCatalogo(String.valueOf(tipoRiesgoDTO.getCveIdTipoRegistro()));
					catalogoDTO.setDesCatalogo(tipoRiesgoDTO.getDesDescripcion());
					tiposRiesgo.put(String.valueOf(tipoRiesgoDTO.getCveIdTipoRegistro()), catalogoDTO);
				}
			}
		}
		if (tiposRiesgo.get(cveTipoRiesgo) != null) {
			catalogoDTOResultado = tiposRiesgo.get(cveTipoRiesgo);
		}
		return catalogoDTOResultado;
	}

	@Override
	public CatalogoDTO obtenerModificacionPatronal(String cveModifPatronal) {
		CatalogoDTO catalogoDTOResultado = new CatalogoDTO();
		if (modificaciones == null) {
			Optional<List<ModificacionPatronalDTO>> modificacionesList = modificacionPatronalRepository.findAll();
			modificaciones = new HashMap<>();
			if (modificacionesList.isPresent()) {
				for (ModificacionPatronalDTO modificacionPatronalDTO : modificacionesList.get()) {
					CatalogoDTO catalogoDTO = new CatalogoDTO();
					catalogoDTO.setCveCatalogo(String.valueOf(modificacionPatronalDTO.getCveIdModifPatron()));
					catalogoDTO.setDesCatalogo(modificacionPatronalDTO.getDesModifPatron());
					modificaciones.put(String.valueOf(modificacionPatronalDTO.getCveIdModifPatron()), catalogoDTO);
			
			}
		}
		}
		if (modificaciones.get(cveModifPatronal) != null) {
			catalogoDTOResultado = modificaciones.get(cveModifPatronal);
		}
		return catalogoDTOResultado;
	
	}
	
	

}
