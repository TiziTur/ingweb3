package ar.edu.iua.iw3.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.iua.iw3.business.IOrdenBusiness;
import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.ConflictException;
import ar.edu.iua.iw3.business.execption.FoundException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.dto.ActivacionRequest;
import ar.edu.iua.iw3.model.dto.Conciliacion;
import ar.edu.iua.iw3.model.dto.DatoCargaRequest;
import ar.edu.iua.iw3.model.dto.PasswordResponse;
import ar.edu.iua.iw3.model.dto.PesajeRequest;
import ar.edu.iua.iw3.model.dto.PresetResponse;
import ar.edu.iua.iw3.util.IStandartResponseBusiness;
import ar.edu.iua.iw3.util.StandartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints de las órdenes de carga (puntos 1 a 5 de la consigna).
 *
 * El controlador no tiene lógica: llama al negocio (IOrdenBusiness) y traduce cada excepción
 * a un código HTTP, igual que ProductRestController del proyecto base:
 *   NotFoundException   -> 404
 *   FoundException      -> 302
 *   BadRequestException -> 400
 *   ConflictException   -> 409 (la orden no está en el estado que corresponde)
 *   BusinessException   -> 500
 *
 * Los roles se controlan con @PreAuthorize. Cada sistema externo tiene su usuario y su rol.
 */
@RestController
@RequestMapping(Constants.URL_ORDENES)
@Tag(name = "Órdenes", description = "Ciclo de vida de una orden de carga de gas (puntos 1 a 5)")
public class OrdenRestController extends BaseRestController {

	@Autowired
	private IOrdenBusiness ordenBusiness;

	@Autowired
	private IStandartResponseBusiness response;

	// =====================================================================
	// Consultas
	// =====================================================================

	@Operation(summary = "Lista todas las órdenes")
	@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
	@GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> list() {
		try {
			return new ResponseEntity<>(ordenBusiness.list(), HttpStatus.OK);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Operation(summary = "Devuelve una orden por su número")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "La orden", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Orden.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe la orden", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
	@GetMapping(value = "/{numeroOrden}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> load(@PathVariable Long numeroOrden) {
		try {
			return new ResponseEntity<>(ordenBusiness.load(numeroOrden), HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Operation(summary = "Detalles de carga guardados de una orden")
	@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
	@GetMapping(value = "/{numeroOrden}/detalles", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> listDetalles(@PathVariable Long numeroOrden) {
		try {
			return new ResponseEntity<>(ordenBusiness.listDetalles(numeroOrden), HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Operation(summary = "Historial de cambios de estado de una orden (auditoría)")
	@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
	@GetMapping(value = "/{numeroOrden}/estados", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> listCambiosEstado(@PathVariable Long numeroOrden) {
		try {
			return new ResponseEntity<>(ordenBusiness.listCambiosEstado(numeroOrden), HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// =====================================================================
	// Punto 1: SAP envía la orden -> estado 1
	// =====================================================================

	@Operation(summary = "Punto 1 - Recepción de la orden desde SAP", description = "Crea la orden en estado 1. "
			+ "Camión, chofer, cliente y producto se crean o actualizan según su codigoExterno.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Orden creada. El header location indica su URL"),
			@ApiResponse(responseCode = "302", description = "Ya existe una orden con ese número"),
			@ApiResponse(responseCode = "400", description = "Faltan datos obligatorios") })
	@PreAuthorize("hasAnyRole('ADMIN','SAP')")
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> recibirOrden(@RequestBody Orden orden) {
		try {
			Orden nueva = ordenBusiness.recibirOrden(orden);
			HttpHeaders headers = new HttpHeaders();
			headers.set("location", Constants.URL_ORDENES + "/" + nueva.getNumeroOrden());
			return new ResponseEntity<>(headers, HttpStatus.CREATED);
		} catch (FoundException e) {
			return error(HttpStatus.FOUND, e);
		} catch (BadRequestException e) {
			return error(HttpStatus.BAD_REQUEST, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// =====================================================================
	// Punto 2: TMS envía la tara -> estado 2
	// =====================================================================

	@Operation(summary = "Punto 2 - Pesaje inicial (tara) desde el TMS", description = "Registra la tara, genera la "
			+ "contraseña de activación de 5 dígitos y la devuelve. La orden pasa a estado 2.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Devuelve el número de orden y la contraseña", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = PasswordResponse.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 1") })
	@PreAuthorize("hasAnyRole('ADMIN','TMS')")
	@PostMapping(value = "/{numeroOrden}/pesaje-inicial", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> pesajeInicial(@PathVariable Long numeroOrden, @RequestBody PesajeRequest pesaje) {
		try {
			Orden orden = ordenBusiness.registrarPesajeInicial(numeroOrden, pesaje.getPeso());
			return new ResponseEntity<>(new PasswordResponse(orden.getNumeroOrden(), orden.getPassword()),
					HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BadRequestException e) {
			return error(HttpStatus.BAD_REQUEST, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// =====================================================================
	// Punto 3: activación de la carga y datos en tiempo real
	// =====================================================================

	@Operation(summary = "Punto 3 - Activación de la carga", description = "El sistema de carga envía la contraseña. "
			+ "Si la orden existe, está en estado 2 y la contraseña es correcta, devuelve el preset.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Devuelve el preset", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = PresetResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Contraseña incorrecta"),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2") })
	@PreAuthorize("hasAnyRole('ADMIN','CARGA')")
	@PostMapping(value = "/{numeroOrden}/activar", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> activar(@PathVariable Long numeroOrden, @RequestBody ActivacionRequest activacion) {
		try {
			Orden orden = ordenBusiness.activarCarga(numeroOrden, activacion.getPassword());
			return new ResponseEntity<>(new PresetResponse(orden.getNumeroOrden(), orden.getPreset()), HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BadRequestException e) {
			return error(HttpStatus.BAD_REQUEST, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Operation(summary = "Punto 3 - Dato de carga en tiempo real", description = "Se descarta (400) si caudal <= 0, "
			+ "masa <= 0 o masa menor a la anterior. Si es válido actualiza los últimos valores de la orden y, "
			+ "según la frecuencia configurada, guarda un registro de detalle.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Dato aceptado"),
			@ApiResponse(responseCode = "400", description = "Dato descartado"),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2 (por ejemplo, ya está cerrada)") })
	@PreAuthorize("hasAnyRole('ADMIN','CARGA')")
	@PostMapping(value = "/{numeroOrden}/detalle", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> detalle(@PathVariable Long numeroOrden, @RequestBody DatoCargaRequest dato) {
		try {
			ordenBusiness.recibirDatoCarga(numeroOrden, dato);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BadRequestException e) {
			return error(HttpStatus.BAD_REQUEST, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// =====================================================================
	// Punto 4: cierre de la orden -> estado 3
	// =====================================================================

	@Operation(summary = "Punto 4 - Cierre de la orden", description = "Bloquea la recepción de nuevos datos de "
			+ "detalle. La orden pasa a estado 3.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Orden cerrada"),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2") })
	@PreAuthorize("hasAnyRole('ADMIN','CARGA')")
	@PostMapping(value = "/{numeroOrden}/cerrar")
	public ResponseEntity<?> cerrar(@PathVariable Long numeroOrden) {
		try {
			ordenBusiness.cerrarOrden(numeroOrden);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// =====================================================================
	// Punto 5: pesaje final y conciliación -> estado 4
	// =====================================================================

	@Operation(summary = "Punto 5 - Pesaje final desde el TMS", description = "Registra el pesaje final, la orden "
			+ "pasa a estado 4 y devuelve la conciliación.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Devuelve la conciliación", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Conciliacion.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 3") })
	@PreAuthorize("hasAnyRole('ADMIN','TMS')")
	@PostMapping(value = "/{numeroOrden}/pesaje-final", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> pesajeFinal(@PathVariable Long numeroOrden, @RequestBody PesajeRequest pesaje) {
		try {
			return new ResponseEntity<>(ordenBusiness.registrarPesajeFinal(numeroOrden, pesaje.getPeso()),
					HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BadRequestException e) {
			return error(HttpStatus.BAD_REQUEST, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Operation(summary = "Punto 5 - Conciliación de una orden finalizada", description = "Solo para órdenes en estado 4.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "La conciliación", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Conciliacion.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe la orden"),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 4") })
	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/{numeroOrden}/conciliacion", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> conciliacion(@PathVariable Long numeroOrden) {
		try {
			return new ResponseEntity<>(ordenBusiness.getConciliacion(numeroOrden), HttpStatus.OK);
		} catch (NotFoundException e) {
			return error(HttpStatus.NOT_FOUND, e);
		} catch (ConflictException e) {
			return error(HttpStatus.CONFLICT, e);
		} catch (BusinessException e) {
			return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	// Arma la respuesta de error estándar del proyecto (StandartResponse) con el código indicado
	private ResponseEntity<?> error(HttpStatus status, Exception e) {
		return new ResponseEntity<>(response.build(status, e, e.getMessage()), status);
	}
}
