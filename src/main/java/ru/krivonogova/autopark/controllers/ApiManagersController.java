package ru.krivonogova.autopark.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import ru.krivonogova.autopark.dto.DriverDTO;
import ru.krivonogova.autopark.dto.GeoJsonFeature;
import ru.krivonogova.autopark.dto.PointGpsDTO;
import ru.krivonogova.autopark.dto.PointGpsDTO_forAPI;
import ru.krivonogova.autopark.dto.ReportRequestDTO;
import ru.krivonogova.autopark.dto.TripDTO_forAPI;
import ru.krivonogova.autopark.dto.VehicleDTO;
import ru.krivonogova.autopark.dto.VehicleDTO_forAPI;
import ru.krivonogova.autopark.models.Driver;
import ru.krivonogova.autopark.models.Enterprise;
import ru.krivonogova.autopark.models.Period;
import ru.krivonogova.autopark.models.PointGps;
import ru.krivonogova.autopark.models.ReportResult;
import ru.krivonogova.autopark.models.Trip;
import ru.krivonogova.autopark.models.TypeReport;
import ru.krivonogova.autopark.models.Vehicle;
import ru.krivonogova.autopark.security.PersonDetails;
import ru.krivonogova.autopark.services.DriversService;
import ru.krivonogova.autopark.services.EnterprisesService;
import ru.krivonogova.autopark.services.ManagersService;
import ru.krivonogova.autopark.services.PointsGpsService;
import ru.krivonogova.autopark.services.ReportsService;
import ru.krivonogova.autopark.services.TripService;
import ru.krivonogova.autopark.services.VehiclesService;
import ru.krivonogova.autopark.util.EnterpriseErrorResponse;
import ru.krivonogova.autopark.util.EnterpriseNotCreatedException;
import ru.krivonogova.autopark.util.EnterpriseNotDeletedException;
import ru.krivonogova.autopark.util.EnterpriseNotUpdatedException;
import ru.krivonogova.autopark.util.VehicleErrorResponse;
import ru.krivonogova.autopark.util.VehicleNotCreatedException;
import ru.krivonogova.autopark.util.VehicleNotFoundException;
import ru.krivonogova.autopark.util.VehicleNotUpdatedException;

@RestController
@RequestMapping("/api/managers")
public class ApiManagersController {
	
	private final EnterprisesService enterprisesService;
	private final VehiclesService vehiclesService;
	private final DriversService driversService;
	private final ModelMapper modelMapper;
	private final ManagersService managersService;
	private final PointsGpsService pointsGpsService;
	private final TripService tripService;
	private final ReportsService reportsService;
	
	@Autowired
	public ApiManagersController(EnterprisesService enterprisesService, VehiclesService vehiclesService,
			DriversService driversService, ModelMapper modelMapper, ManagersService managersService,
			PointsGpsService pointsGpsService, TripService tripService, ReportsService reportsService) {
		this.enterprisesService = enterprisesService;
		this.vehiclesService = vehiclesService;
		this.driversService = driversService;
		this.modelMapper = modelMapper;
		this.managersService = managersService;
		this.pointsGpsService = pointsGpsService;
		this.tripService = tripService;
		this.reportsService = reportsService;
	}
	
	// время в UTC = 0 (как исходно хранятся в таблице)
	@GetMapping("/allpoints")
	public List<PointGpsDTO> indexAllPointsGPS() {
						
		return pointsGpsService.findAll().stream().map(this::convertToPointGpsDTO)
				.collect(Collectors.toList());
	}
	
	// время (возвращается) с учетом таймзоны предприятия, в запросе в UTC время
	@GetMapping("/points")
	public Object indexPointsGPS(@RequestParam(value = "vehicleId", defaultValue = "1") int vehicleId,
								@RequestParam(value = "dateFrom", defaultValue = "") String dateFrom,
								@RequestParam(value = "dateTo", defaultValue = "") String dateTo,
								@RequestParam(value = "returnGeoJson", defaultValue = "false") boolean returnGeoJson) {
				
		List<PointGps> points = pointsGpsService.findAllByVehicleAndTimePeriod(vehicleId, dateFrom, dateTo);
		
		 if (returnGeoJson) {
			return points.stream().map(this::convertToPointGpsDTO_forAPI)
									.map(this::convertToGeoJsonFeature)
									.collect(Collectors.toList());
		 }
		
		return points.stream().map(this::convertToPointGpsDTO_forAPI)
								.collect(Collectors.toList());
	}
	
	// время (возвращается) с учетом таймзоны предприятия, в запросе в UTC время
	@GetMapping("/trips/points")
	public Object indexPointsGPSFromTrip(@RequestParam(value = "vehicleId", defaultValue = "1") int vehicleId,
										@RequestParam(value = "dateFrom", defaultValue = "") String dateFrom,
										@RequestParam(value = "dateTo", defaultValue = "") String dateTo,
										@RequestParam(value = "returnGeoJson", defaultValue = "false") boolean returnGeoJson) {
		
		List<Trip> trips = tripService.findAllByTimePeriod(vehicleId, dateFrom, dateTo);
		
		List<PointGps> points = new ArrayList<PointGps>();
		
		if(!trips.isEmpty()) {
			points = pointsGpsService.findAllByVehicleAndTrip(vehicleId, trips);
		}
		
		if (returnGeoJson) {
			return points.stream().map(this::convertToPointGpsDTO_forAPI)
									.map(this::convertToGeoJsonFeature)
									.collect(Collectors.toList());
		 }
		
		return points.stream().map(this::convertToPointGpsDTO_forAPI)
								.collect(Collectors.toList());
		
	}
	
	// время (возвращается) с учетом таймзоны предприятия, в запросе время предприятия!
	@GetMapping("/trips")
	public List<TripDTO_forAPI> indexPointsTrip(@RequestParam(value = "vehicleId", defaultValue = "1") int vehicleId,
								@RequestParam(value = "dateFrom", defaultValue = "") String dateFrom,
								@RequestParam(value = "dateTo", defaultValue = "") String dateTo) throws ParseException {
		
		String timezone_enter = vehiclesService.findOne(vehicleId).getEnterprise().getTimezone();

		TimeZone timezone = TimeZone.getTimeZone("GMT" + timezone_enter);
		SimpleDateFormat sdfLocal = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		sdfLocal.setTimeZone(timezone);

		SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date fromDate = sdfLocal.parse(dateFrom);
		Date toDate = sdfLocal.parse(dateTo);

		String dateFromUTC = sdfUTC.format(fromDate);
		String dateToUTC = sdfUTC.format(toDate);
		
		List<Trip> trips = tripService.findAllByTimePeriod(vehicleId, dateFromUTC, dateToUTC);
		
		if(trips.isEmpty()) {
			return new ArrayList<TripDTO_forAPI>();
		}
		
		return trips.stream().map(this::convertToTripDTO_forAPI)
								.collect(Collectors.toList());
		
	}
	
	@GetMapping("/report")
	public List<ReportResult> indexResult(@RequestParam int idVehicle,
            @RequestParam TypeReport typeReport,
            @RequestParam Period period,
            @RequestParam String dateFrom,
            @RequestParam String dateTo) throws ParseException {
		
		ReportRequestDTO request = new ReportRequestDTO(idVehicle, typeReport, period, dateFrom, dateTo);
		
		String timezone_enter = vehiclesService.findOne(request.getIdVehicle()).getEnterprise().getTimezone();

		TimeZone timezone = TimeZone.getTimeZone("GMT" + timezone_enter);
		SimpleDateFormat sdfLocal = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		sdfLocal.setTimeZone(timezone);

		SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date fromDate = sdfLocal.parse(request.getDateFrom());
		Date toDate = sdfLocal.parse(request.getDateTo());

		String dateFromUTC = sdfUTC.format(fromDate);
		String dateToUTC = sdfUTC.format(toDate);
		
		List<Trip> trips = tripService.findAllByTimePeriod(request.getIdVehicle(), dateFromUTC, dateToUTC);
		
		return reportsService.getReport(request, trips);
	}
		
	// время в таймзоне предприятия
	private TripDTO_forAPI convertToTripDTO_forAPI(Trip trip) {
		
		TripDTO_forAPI tripDTO = modelMapper.map(trip, TripDTO_forAPI.class);
				
		// установка времени
		String timeOfStart = trip.getTimeOfStart();
		String timeOfEnd = trip.getTimeOfEnd();
		
		String timezone = trip.getVehicle().getEnterprise().getTimezone();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	    LocalDateTime dateOfStart_UTC = LocalDateTime.parse(timeOfStart, formatter);
	    LocalDateTime dateOfEnd_UTC = LocalDateTime.parse(timeOfEnd, formatter);
	    
	    ZoneOffset timeZone = ZoneOffset.of(timezone);
	    
	    LocalDateTime dateOfStart = dateOfStart_UTC.atZone(ZoneOffset.UTC).withZoneSameInstant(timeZone).toLocalDateTime();
	    LocalDateTime dateOfEnd = dateOfEnd_UTC.atZone(ZoneOffset.UTC).withZoneSameInstant(timeZone).toLocalDateTime();
	    
	    String dateOfStartForEnterprise = dateOfStart.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
	    String dateOfEndForEnterprise = dateOfEnd.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
	    
	    tripDTO.setTimeOfStartForEnterprise(dateOfStartForEnterprise);
	    tripDTO.setTimeOfEndForEnterprise(dateOfEndForEnterprise);
	    
		// установка адресов		
		Optional<PointGps> pointOfStart = pointsGpsService.findByVehicleAndTime(trip.getVehicle().getId(), timeOfStart);
		Optional<PointGps> pointOfEnd = pointsGpsService.findByVehicleAndTime(trip.getVehicle().getId(), timeOfEnd);
		
		String addressOfStart = pointsGpsService.takeAddressOfPointGPS(pointOfStart.get());
		String addressOfEnd = pointsGpsService.takeAddressOfPointGPS(pointOfEnd.get());

		tripDTO.setAddressOfStart(addressOfStart);
		tripDTO.setAddressOfEnd(addressOfEnd);
				
		return tripDTO;
	}
	
	private GeoJsonFeature convertToGeoJsonFeature(PointGpsDTO_forAPI point) {
	    GeoJsonFeature feature = new GeoJsonFeature();
	    feature.setType("Feature");

	    Map<String, Object> geometry = new HashMap<>();
	    geometry.put("type", "Point");
        
        List<Double> coordinates = Arrays.asList(point.getLongitude(), point.getLatitude());
        geometry.put("coordinates", coordinates);
        
        geometry.put("idVehicle", point.getVehicleId().getId());
        
        feature.setGeometry(geometry);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("crs", "EPSG:4326");
        
        feature.setProperties(properties);
        
        feature.setTimeOfPointGps(point.getTimeOfPointGpsForEnterprise());
        
	    return feature;
	}
	
	/*@GetMapping("/points")
	public List<PointGpsDTO> indexPointsGPS(@RequestParam(value = "vehicleId", defaultValue = "1") int vehicleId) {
						
		return pointsGpsService.findAllByVehicleId(vehicleId).stream().map(this::convertToPointGpsDTO)
				.collect(Collectors.toList());
	}*/
		
	private PointGpsDTO convertToPointGpsDTO(PointGps pointGps) {
		return modelMapper.map(pointGps, PointGpsDTO.class);
	}
	
	@GetMapping
	public ModelAndView start(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		//ManagerDetails managerDetails = (ManagerDetails) authentication.getPrincipal();
		
		//System.out.println(managerDetails.getManager().getUsername());
		
		//model.addAttribute("manager", managerDetails.getManager());
		
		PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
		
		String username = personDetails.getPerson().getUsername();
		System.out.println(username);
		
		model.addAttribute("manager", managersService.findByUsername(username));
		
		return new ModelAndView("start"); 
	}
	
	@GetMapping("/{id}/enterprises")
	public List<Enterprise> indexEnterprises(@PathVariable("id") int id) {
		return enterprisesService.findAllForManager(id);
	}
		
// так было	
//	@GetMapping("/{id}/vehicles")
//	public List<VehicleDTO> indexVehicles(@PathVariable("id") int id) {
//		return vehiclesService.findAllForManager(id).stream().map(this::convertToVehicleDTO)
//        		.collect(Collectors.toList());
//	}
	
	@GetMapping("/{id}/vehicles")
	public List<VehicleDTO_forAPI> indexVehicles(@PathVariable("id") int id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "itemsPerPage", required = false) Integer itemsPerPage) {
		
		if(page == null || itemsPerPage == null) {
			return vehiclesService.findAllForManager(id).stream().map(this::convertToVehicleDTO_forAPI)
	        		.collect(Collectors.toList());
		}
		
		Page<Vehicle> vehiclePage = vehiclesService.findAllForManager(id, page, itemsPerPage);
		List<VehicleDTO_forAPI> vehicleDTOList = vehiclePage.getContent().stream().map(this::convertToVehicleDTO_forAPI)
				.collect(Collectors.toList());
		
		return vehicleDTOList;
	}
	
	private VehicleDTO_forAPI convertToVehicleDTO_forAPI(Vehicle vehicle) {
		String timezone = vehicle.getEnterprise().getTimezone();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
	    LocalDateTime dateOfSale_UTC = LocalDateTime.parse(vehicle.getDateOfSale(), formatter);
	    ZoneOffset timeZone = ZoneOffset.of(timezone);
	    LocalDateTime dateOfSale = dateOfSale_UTC.atZone(ZoneOffset.UTC).withZoneSameInstant(timeZone).toLocalDateTime();
	    String dateOfSaleForEnterprise = dateOfSale.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
	    
	    VehicleDTO_forAPI vehicleDTO_forAPI = modelMapper.map(vehicle, VehicleDTO_forAPI.class);
	    vehicleDTO_forAPI.setDateOfSaleForEnterprise(dateOfSaleForEnterprise);
	    
		return vehicleDTO_forAPI;
	}
	
	private PointGpsDTO_forAPI convertToPointGpsDTO_forAPI(PointGps pointGps) {
		Vehicle vehicle = pointGps.getVehicle();
		String timezone = vehicle.getEnterprise().getTimezone();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
	    LocalDateTime timeOfPointGps_UTC = LocalDateTime.parse(pointGps.getTimeOfPointGps(), formatter);
	    ZoneOffset timeZone = ZoneOffset.of(timezone);
	    LocalDateTime timeOfPointGps = timeOfPointGps_UTC.atZone(ZoneOffset.UTC).withZoneSameInstant(timeZone).toLocalDateTime();
	    String timeOfPointGpsForEnterprise = timeOfPointGps.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
	    
	    PointGpsDTO_forAPI pointGpsDTO_forAPI = modelMapper.map(pointGps, PointGpsDTO_forAPI.class);
	    pointGpsDTO_forAPI.setTimeOfPointGpsForEnterprise(timeOfPointGpsForEnterprise);
	    
	    return pointGpsDTO_forAPI;
	}
	
	private VehicleDTO convertToVehicleDTO(Vehicle vehicle) {
		return modelMapper.map(vehicle, VehicleDTO.class);
	}
	
// так было	
//	@GetMapping("/{id}/drivers")
//	public List<DriverDTO> indexDrivers(@PathVariable("id") int id) {
//		return driversService.findAllForManager(id).stream().map(this::convertToDriverDTO)
//        		.collect(Collectors.toList());
//	}
	
	@GetMapping("/{id}/drivers")
	public List<DriverDTO> indexDrivers(@PathVariable("id") int id,
					@RequestParam(value = "page", required = false) Integer page,
					@RequestParam(value = "itemsPerPage", required = false) Integer itemsPerPage) {
		
		if(page == null || itemsPerPage == null) {
			return driversService.findAllForManager(id).stream().map(this::convertToDriverDTO)
	        		.collect(Collectors.toList());			
		}
		
		Page<Driver> driverPage = driversService.findAllForManager(id, page, itemsPerPage);
		List<DriverDTO> driverDTOList = driverPage.getContent().stream().map(this::convertToDriverDTO)
	                .collect(Collectors.toList());
		
	    return driverDTOList;
	}
	
	private DriverDTO convertToDriverDTO(Driver driver) {
		return modelMapper.map(driver, DriverDTO.class);
	}
	
	@PostMapping("/{id}/vehicles")
	public ResponseEntity<HttpStatus> create(@RequestBody @Valid VehicleDTO vehicle,
											BindingResult bindingResult) {
		
		if(bindingResult.hasErrors()) {
    		StringBuilder errorMsg = new StringBuilder();
    		
    		List<FieldError> errors = bindingResult.getFieldErrors();
    		
    		for(FieldError error : errors) {
    			errorMsg.append(error.getField())
    					.append(" - ")
    					.append(error.getDefaultMessage())
    					.append(";");
    		}
    		
    		throw new VehicleNotCreatedException(errorMsg.toString());
    	}
		
		vehiclesService.save(convertToVehicle(vehicle));
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	@PostMapping("/{id}/enterprises")
	public ResponseEntity<HttpStatus> create(@RequestBody @Valid Enterprise enterprise,
											BindingResult bindingResult,
											@PathVariable("id") int id) {
		
		if(bindingResult.hasErrors()) {
    		StringBuilder errorMsg = new StringBuilder();
    		
    		List<FieldError> errors = bindingResult.getFieldErrors();
    		
    		for(FieldError error : errors) {
    			errorMsg.append(error.getField())
    					.append(" - ")
    					.append(error.getDefaultMessage())
    					.append(";");
    		}
    		
    		throw new EnterpriseNotCreatedException(errorMsg.toString());
    	}
		
		enterprisesService.save(enterprise, id);
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	@PutMapping("/{id}/vehicles/{idVehicle}")
	public ResponseEntity<HttpStatus> update(@RequestBody @Valid VehicleDTO vehicle,
											BindingResult bindingResult,
											@PathVariable("idVehicle") int id) {
		if(bindingResult.hasErrors()) {
			StringBuilder errorMsg = new StringBuilder();
    		
    		List<FieldError> errors = bindingResult.getFieldErrors();
    		
    		for(FieldError error : errors) {
    			errorMsg.append(error.getField())
    					.append(" - ")
    					.append(error.getDefaultMessage())
    					.append(";");
    		}
    		
    		throw new VehicleNotUpdatedException(errorMsg.toString());
		}
		
		vehiclesService.update(id, convertToVehicle(vehicle));
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	@PutMapping("/{id}/enterprises/{idEnterprise}")
	public ResponseEntity<HttpStatus> update(@PathVariable("id") int idManager,
											@RequestBody @Valid Enterprise enterprise,
											BindingResult bindingResult,
											@PathVariable("idEnterprise") int idEnterprise) {
		if(bindingResult.hasErrors()) {
			StringBuilder errorMsg = new StringBuilder();
    		
    		List<FieldError> errors = bindingResult.getFieldErrors();
    		
    		for(FieldError error : errors) {
    			errorMsg.append(error.getField())
    					.append(" - ")
    					.append(error.getDefaultMessage())
    					.append(";");
    		}
    		
    		throw new EnterpriseNotUpdatedException(errorMsg.toString());
		}
		
		enterprisesService.update(idManager, idEnterprise, enterprise);
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	@DeleteMapping("/{id}/vehicles/{idVehicle}")
	public ResponseEntity<HttpStatus> deleteVehicle(@PathVariable("idVehicle") int id) {
		vehiclesService.delete(id);
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	@DeleteMapping("/{id}/enterprises/{idEnterprise}")
	public ResponseEntity<HttpStatus> delete(@PathVariable("id") int idManager,
											@PathVariable("idEnterprise") int idEnterprise) {
		
		enterprisesService.delete(idManager, idEnterprise);
		
		return ResponseEntity.ok(HttpStatus.OK);
	}
	
	private Vehicle convertToVehicle(VehicleDTO vehicleDTO) {
		return modelMapper.map(vehicleDTO, Vehicle.class);
	}
	
	@ExceptionHandler
    private ResponseEntity<VehicleErrorResponse> handlerException(VehicleNotFoundException e) {
    	VehicleErrorResponse response = new VehicleErrorResponse(
    			"Vehicle with this id wasn't found", 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.NOT_FOUND); // статус 404
    }	
		
    @ExceptionHandler
    private ResponseEntity<VehicleErrorResponse> handlerException(VehicleNotCreatedException e) {
    	VehicleErrorResponse response = new VehicleErrorResponse(
    			e.getMessage(), 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // статус 400
    }
    
    @ExceptionHandler
    private ResponseEntity<VehicleErrorResponse> handlerException(VehicleNotUpdatedException e) {
    	VehicleErrorResponse response = new VehicleErrorResponse(
    			e.getMessage(), 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // статус 400
    }
    
    @ExceptionHandler
    private ResponseEntity<EnterpriseErrorResponse> handlerException(EnterpriseNotCreatedException e) {
    	EnterpriseErrorResponse response = new EnterpriseErrorResponse(
    			e.getMessage(), 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // статус 400
    }
    
    @ExceptionHandler
    private ResponseEntity<EnterpriseErrorResponse> handlerException(EnterpriseNotUpdatedException e) {
    	EnterpriseErrorResponse response = new EnterpriseErrorResponse(
    			e.getMessage(), 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.FORBIDDEN); // статус 403
    }
    
    @ExceptionHandler
    private ResponseEntity<EnterpriseErrorResponse> handlerException(EnterpriseNotDeletedException e) {
    	EnterpriseErrorResponse response = new EnterpriseErrorResponse(
    			e.getMessage(), 
    			System.currentTimeMillis()
    	);
    	
    	return new ResponseEntity<>(response, HttpStatus.FORBIDDEN); // статус 403
    }

}
