package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import java.util.Optional;

import mx.gob.imss.cit.mspmccommons.dto.ConsecuenciaDTO;

public interface ConsecuenciaRepository {

	Optional<ConsecuenciaDTO> findOneByCve(String cveConsecuencia);
	
	Optional<List<ConsecuenciaDTO>> findAll();

}
