package ar.edu.iua.iw3.business;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.ConflictException;
import ar.edu.iua.iw3.business.execption.FoundException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.CambioEstado;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.dto.Conciliacion;
import ar.edu.iua.iw3.model.dto.DatoCargaRequest;
import ar.edu.iua.iw3.model.persistence.CambioEstadoRepository;
import ar.edu.iua.iw3.model.persistence.DetalleCargaRepository;
import ar.edu.iua.iw3.model.persistence.OrdenRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Toda la lógica del ciclo de vida de una orden (puntos 1 a 5 de la consigna).
 *
 * Regla general: cada método de un punto primero busca la orden, después controla que esté
 * en el estado que corresponde, y recién ahí modifica datos y la pasa al estado siguiente.
 * El cambio de estado se hace siempre con el método cambiarEstado(), que además deja
 * registrado el cambio en la tabla de auditoría (cambios_estado).
 */
@Service
@Slf4j
public class OrdenBusiness implements IOrdenBusiness {

	@Autowired
	private OrdenRepository ordenDAO;

	@Autowired
	private DetalleCargaRepository detalleDAO;

	@Autowired
	private CambioEstadoRepository cambioEstadoDAO;

	@Autowired
	private ICamionBusiness camionBusiness;

	@Autowired
	private IChoferBusiness choferBusiness;

	@Autowired
	private IClienteBusiness clienteBusiness;

	@Autowired
	private IProductoBusiness productoBusiness;

	// Cada cuántos segundos se guarda un detalle (application.properties)
	@Value("${carga.detalle.frecuencia.segundos:10}")
	private long frecuenciaDetalleSegundos;

	// =====================================================================
	// Consultas
	// =====================================================================

	@Override
	public List<Orden> list() throws BusinessException {
		try {
			return ordenDAO.findAll();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Orden load(Long numeroOrden) throws NotFoundException, BusinessException {
		Optional<Orden> r;
		try {
			r = ordenDAO.findByNumeroOrden(numeroOrden);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra la orden numero=" + numeroOrden).build();
		}
		return r.get();
	}

	@Override
	public List<DetalleCarga> listDetalles(Long numeroOrden) throws NotFoundException, BusinessException {
		Orden orden = load(numeroOrden);
		try {
			return detalleDAO.findByOrdenIdOrderByFechaAsc(orden.getId());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public List<CambioEstado> listCambiosEstado(Long numeroOrden) throws NotFoundException, BusinessException {
		Orden orden = load(numeroOrden);
		try {
			return cambioEstadoDAO.findByOrdenIdOrderByFechaAsc(orden.getId());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// =====================================================================
	// Punto 1: recepción de la orden desde SAP
	// =====================================================================

	@Override
	public Orden recibirOrden(Orden orden) throws FoundException, BadRequestException, BusinessException {
		// 1) Validar los datos obligatorios
		if (orden.getNumeroOrden() == null) {
			throw BadRequestException.builder().message("Falta el numeroOrden").build();
		}
		if (orden.getPreset() == null || orden.getPreset() <= 0) {
			throw BadRequestException.builder().message("El preset debe ser mayor a 0").build();
		}

		// 2) No puede existir otra orden con el mismo número
		try {
			load(orden.getNumeroOrden());
			throw FoundException.builder().message("Ya existe la orden numero=" + orden.getNumeroOrden()).build();
		} catch (NotFoundException e) {
			// No existe: es lo esperado, seguimos
		}

		// 3) Sincronizar las entidades externas por su código externo (se crean o se actualizan)
		orden.setCamion(camionBusiness.sincronizar(orden.getCamion()));
		orden.setChofer(choferBusiness.sincronizar(orden.getChofer()));
		orden.setCliente(clienteBusiness.sincronizar(orden.getCliente()));
		orden.setProducto(productoBusiness.sincronizar(orden.getProducto()));

		// 4) Los datos internos no los define SAP: se limpian por si vinieron en el JSON
		orden.setId(null);
		orden.setTara(null);
		orden.setPesajeFinal(null);
		orden.setPassword(null);
		orden.setFechaPesajeInicial(null);
		orden.setFechaInicioCarga(null);
		orden.setFechaFinCarga(null);
		orden.setFechaPesajeFinal(null);
		orden.setUltimaMasaAcumulada(null);
		orden.setUltimaDensidad(null);
		orden.setUltimaTemperatura(null);
		orden.setUltimoCaudal(null);
		orden.setFechaUltimoDato(null);

		// 5) Estado 1 y fecha de recepción
		orden.setFechaRecepcion(new Date());
		orden.setEstado(EstadoOrden.PENDIENTE_PESAJE_INICIAL);
		Orden guardada = guardar(orden);
		registrarCambioEstado(guardada, null, EstadoOrden.PENDIENTE_PESAJE_INICIAL, "Orden recibida");
		return guardada;
	}

	// =====================================================================
	// Punto 2: pesaje inicial (tara) desde el TMS
	// =====================================================================

	@Override
	public Orden registrarPesajeInicial(Long numeroOrden, Double tara)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException {
		if (tara == null || tara <= 0) {
			throw BadRequestException.builder().message("El peso debe ser mayor a 0").build();
		}
		Orden orden = load(numeroOrden);
		controlarEstado(orden, EstadoOrden.PENDIENTE_PESAJE_INICIAL);

		orden.setTara(tara);
		orden.setFechaPesajeInicial(new Date());
		orden.setPassword(generarPassword());
		cambiarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL, "Tara registrada: " + tara + " kg");
		return orden;
	}

	// Número entero de 5 dígitos: entre 10000 y 99999
	private int generarPassword() {
		return 10000 + (int) (Math.random() * 90000);
	}

	// =====================================================================
	// Punto 3: activación de la carga (password -> preset)
	// =====================================================================

	@Override
	public Orden activarCarga(Long numeroOrden, Integer password)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException {
		Orden orden = load(numeroOrden);
		controlarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL);
		if (password == null || !password.equals(orden.getPassword())) {
			throw BadRequestException.builder().message("Password de activación incorrecta").build();
		}
		// No cambia el estado: la orden sigue en estado 2 mientras se carga
		return orden;
	}

	// =====================================================================
	// Punto 3: datos de carga en tiempo real
	// =====================================================================

	@Override
	public Orden recibirDatoCarga(Long numeroOrden, DatoCargaRequest dato)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException {
		Orden orden = load(numeroOrden);
		// Si la orden ya está cerrada (estado 3) se rechaza: así se "bloquea" la recepción
		controlarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL);

		// 1) Validaciones de la consigna: si no se cumplen, el dato se descarta
		if (dato.getMasaAcumulada() == null || dato.getDensidad() == null || dato.getTemperatura() == null
				|| dato.getCaudal() == null) {
			throw BadRequestException.builder().message("Dato descartado: faltan valores").build();
		}
		if (dato.getCaudal() <= 0) {
			throw BadRequestException.builder().message("Dato descartado: caudal <= 0").build();
		}
		if (dato.getMasaAcumulada() <= 0) {
			throw BadRequestException.builder().message("Dato descartado: masa acumulada <= 0").build();
		}
		if (orden.getUltimaMasaAcumulada() != null && dato.getMasaAcumulada() < orden.getUltimaMasaAcumulada()) {
			throw BadRequestException.builder().message("Dato descartado: masa acumulada menor a la anterior").build();
		}

		// 2) El dato es válido: actualizamos la cabecera con los últimos valores (siempre)
		Date ahora = new Date();
		if (orden.getFechaInicioCarga() == null) {
			orden.setFechaInicioCarga(ahora); // primer dato válido
		}
		orden.setFechaFinCarga(ahora); // por ahora este es el último dato válido
		orden.setUltimaMasaAcumulada(dato.getMasaAcumulada());
		orden.setUltimaDensidad(dato.getDensidad());
		orden.setUltimaTemperatura(dato.getTemperatura());
		orden.setUltimoCaudal(dato.getCaudal());
		orden.setFechaUltimoDato(ahora);
		Orden guardada = guardar(orden);

		// 3) El detalle se guarda solo si pasó el tiempo de la frecuencia configurada
		if (correspondeGuardarDetalle(guardada, ahora)) {
			DetalleCarga d = new DetalleCarga();
			d.setOrden(guardada);
			d.setMasaAcumulada(dato.getMasaAcumulada());
			d.setDensidad(dato.getDensidad());
			d.setTemperatura(dato.getTemperatura());
			d.setCaudal(dato.getCaudal());
			d.setFecha(ahora);
			try {
				detalleDAO.save(d);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw BusinessException.builder().ex(e).build();
			}
		}
		return guardada;
	}

	// true si la orden no tiene detalles o si el último se guardó hace "frecuencia" segundos o más
	private boolean correspondeGuardarDetalle(Orden orden, Date ahora) throws BusinessException {
		try {
			Optional<DetalleCarga> ultimo = detalleDAO.findFirstByOrdenIdOrderByFechaDesc(orden.getId());
			if (ultimo.isEmpty()) {
				return true;
			}
			long segundos = (ahora.getTime() - ultimo.get().getFecha().getTime()) / 1000;
			return segundos >= frecuenciaDetalleSegundos;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// =====================================================================
	// Punto 4: cierre de la orden
	// =====================================================================

	@Override
	public Orden cerrarOrden(Long numeroOrden) throws NotFoundException, ConflictException, BusinessException {
		Orden orden = load(numeroOrden);
		controlarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL);
		cambiarEstado(orden, EstadoOrden.CERRADA, "Orden cerrada para carga");
		return orden;
	}

	// =====================================================================
	// Punto 5: pesaje final y conciliación
	// =====================================================================

	@Override
	public Conciliacion registrarPesajeFinal(Long numeroOrden, Double pesajeFinal)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException {
		if (pesajeFinal == null || pesajeFinal <= 0) {
			throw BadRequestException.builder().message("El peso debe ser mayor a 0").build();
		}
		Orden orden = load(numeroOrden);
		controlarEstado(orden, EstadoOrden.CERRADA);

		orden.setPesajeFinal(pesajeFinal);
		orden.setFechaPesajeFinal(new Date());
		cambiarEstado(orden, EstadoOrden.FINALIZADA, "Pesaje final registrado: " + pesajeFinal + " kg");
		return armarConciliacion(orden);
	}

	@Override
	public Conciliacion getConciliacion(Long numeroOrden) throws NotFoundException, ConflictException, BusinessException {
		Orden orden = load(numeroOrden);
		controlarEstado(orden, EstadoOrden.FINALIZADA);
		return armarConciliacion(orden);
	}

	private Conciliacion armarConciliacion(Orden orden) throws BusinessException {
		Conciliacion c = new Conciliacion();
		c.setNumeroOrden(orden.getNumeroOrden());
		c.setPesajeInicial(orden.getTara());
		c.setPesajeFinal(orden.getPesajeFinal());

		// Si nunca llegó un dato de carga, el producto cargado es 0
		double productoCargado = orden.getUltimaMasaAcumulada() == null ? 0 : orden.getUltimaMasaAcumulada();
		double neto = orden.getPesajeFinal() - orden.getTara();
		c.setProductoCargado(productoCargado);
		c.setNetoPorBalanza(neto);
		c.setDiferenciaBalanzaCaudalimetro(neto - productoCargado);

		try {
			c.setPromedioTemperatura(detalleDAO.promedioTemperatura(orden.getId()));
			c.setPromedioDensidad(detalleDAO.promedioDensidad(orden.getId()));
			c.setPromedioCaudal(detalleDAO.promedioCaudal(orden.getId()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		return c;
	}

	// =====================================================================
	// Métodos de apoyo
	// =====================================================================

	// Si la orden no está en el estado esperado, la acción no corresponde (409)
	private void controlarEstado(Orden orden, EstadoOrden esperado) throws ConflictException {
		if (orden.getEstado() != esperado) {
			throw ConflictException.builder()
					.message("La orden " + orden.getNumeroOrden() + " está en estado " + orden.getEstado()
							+ " y para esta acción debe estar en " + esperado)
					.build();
		}
	}

	// Pasa la orden al estado nuevo, la guarda y deja registro en la auditoría
	private void cambiarEstado(Orden orden, EstadoOrden nuevo, String observacion) throws BusinessException {
		EstadoOrden anterior = orden.getEstado();
		orden.setEstado(nuevo);
		guardar(orden);
		registrarCambioEstado(orden, anterior, nuevo, observacion);
	}

	private void registrarCambioEstado(Orden orden, EstadoOrden anterior, EstadoOrden nuevo, String observacion)
			throws BusinessException {
		CambioEstado ce = new CambioEstado();
		ce.setOrden(orden);
		ce.setEstadoAnterior(anterior);
		ce.setEstadoNuevo(nuevo);
		ce.setFecha(new Date());
		ce.setUsuario(usuarioActual());
		ce.setObservacion(observacion);
		try {
			cambioEstadoDAO.save(ce);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// Nombre del usuario logueado (sale del token JWT). Si no hay usuario, se registra "sistema".
	private String usuarioActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return "sistema";
		}
		return auth.getName();
	}

	private Orden guardar(Orden orden) throws BusinessException {
		try {
			return ordenDAO.save(orden);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}
}
