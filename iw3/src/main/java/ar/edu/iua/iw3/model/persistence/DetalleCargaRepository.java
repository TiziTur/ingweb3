package ar.edu.iua.iw3.model.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.DetalleCarga;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Long> {

	// Todos los detalles de una orden, del más viejo al más nuevo
	List<DetalleCarga> findByOrdenIdOrderByFechaAsc(Long idOrden);

	// El último detalle guardado de una orden (para saber si ya pasó el tiempo de la frecuencia)
	Optional<DetalleCarga> findFirstByOrdenIdOrderByFechaDesc(Long idOrden);

	// Promedios para la conciliación (punto 5). Devuelven null si la orden no tiene detalles.
	@Query("SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE d.orden.id = ?1")
	Double promedioTemperatura(Long idOrden);

	@Query("SELECT AVG(d.densidad) FROM DetalleCarga d WHERE d.orden.id = ?1")
	Double promedioDensidad(Long idOrden);

	@Query("SELECT AVG(d.caudal) FROM DetalleCarga d WHERE d.orden.id = ?1")
	Double promedioCaudal(Long idOrden);
}
