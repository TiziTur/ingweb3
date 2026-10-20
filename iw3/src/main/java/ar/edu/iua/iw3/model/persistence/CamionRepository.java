package ar.edu.iua.iw3.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.Camion;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {

	// Spring arma la consulta solo a partir del nombre del método:
	// SELECT * FROM ... WHERE codigo_externo = ?
	Optional<Camion> findByCodigoExterno(String codigoExterno);
}
