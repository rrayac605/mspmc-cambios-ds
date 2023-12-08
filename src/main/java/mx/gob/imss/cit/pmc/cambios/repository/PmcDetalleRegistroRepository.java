package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;

import org.bson.types.ObjectId;

import mx.gob.imss.cit.mspmccommons.dto.ArchivoDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;

public interface PmcDetalleRegistroRepository {

	boolean existeRegistro(DetalleRegistroDTO registroDTO);

	List<DetalleRegistroDTO> existeSusceptible(DetalleRegistroDTO registroDTO);
	
	List<DetalleRegistroDTO> existeSusceptibleNssByEstado(DetalleRegistroDTO registroDTO);

	List<DetalleRegistroDTO> existeSusceptibleNss(DetalleRegistroDTO registroDTO);
	
	List<CambioDTO> existeSusceptibleNssCambios(DetalleRegistroDTO registroDTO);
	
	List<CambioDTO> existeSusceptibleNssCambiosByEstado(DetalleRegistroDTO registroDTO);
	
	ArchivoDTO getArchivoByIdMovimiento(ObjectId objectIdMovimiento);

}
