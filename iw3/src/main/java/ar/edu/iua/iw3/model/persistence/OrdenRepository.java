package ar.edu.iua.iw3.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.Orden;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

	Optional<Orden> findByNumeroOrden(Long numeroOrden);
}
