package ar.edu.iua.iw3.model.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.CambioEstado;

@Repository
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

	List<CambioEstado> findByOrdenIdOrderByFechaAsc(Long idOrden);
}
