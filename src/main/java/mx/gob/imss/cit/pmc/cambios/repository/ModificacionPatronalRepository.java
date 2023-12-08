package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import java.util.Optional;

import mx.gob.imss.cit.mspmccommons.dto.ModificacionPatronalDTO;

public interface ModificacionPatronalRepository {
	Optional<ModificacionPatronalDTO> findOneByCve(String cveIdModifPatron);

	Optional<List<ModificacionPatronalDTO>> findAll();
}
