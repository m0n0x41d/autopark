package ru.krivonogova.autopark.controllers;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.krivonogova.autopark.dto.VehicleDTO;
import ru.krivonogova.autopark.models.Vehicle;
import ru.krivonogova.autopark.security.PersonDetails;
import ru.krivonogova.autopark.services.EnterprisesService;
import ru.krivonogova.autopark.services.ManagersService;
import ru.krivonogova.autopark.services.VehiclesService;

@Controller
@RequestMapping("/managers")
public class ManagersController {

	private final EnterprisesService enterprisesService;
	private final ManagersService managersService;
	private final VehiclesService vehiclesService;
	private final ModelMapper modelMapper;
	
	@Autowired	
	public ManagersController(EnterprisesService enterprisesService, ManagersService managersService, VehiclesService vehiclesService, ModelMapper modelMapper) {
		this.enterprisesService = enterprisesService;
		this.managersService = managersService;
		this.vehiclesService = vehiclesService;
		this.modelMapper = modelMapper;
	}

	@GetMapping("/enterprises")
	public ModelAndView indexEnterprisesWOid() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
		String username = personDetails.getPerson().getUsername();
		
		Integer id = managersService.findByUsername(username).getId();
		
		ModelAndView enterprises = new ModelAndView("enterprises/index");
		
		enterprises.addObject("enterprises", enterprisesService.findAllForManager(id));
		
		return enterprises;
	}
	
	@GetMapping("/enterprises/{id}/vehicles")
	public String indexVehicles(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
								@RequestParam(value = "itemsPerPage", required = false, defaultValue = "10") Integer itemsPerPage,
								@PathVariable("id") int idEnterprise,
								Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
		String username = personDetails.getPerson().getUsername();
		
		Integer idManager = managersService.findByUsername(username).getId();
				
		Page<Vehicle> vehiclesPage = vehiclesService.findForManagerByEnterpriseId(idManager, idEnterprise, page, itemsPerPage);
	    model.addAttribute("vehicles", vehiclesPage.getContent().stream().map(this::convertToVehicleDTO).collect(Collectors.toList()));
	    model.addAttribute("currentPage", vehiclesPage.getNumber() + 1);
	    model.addAttribute("totalPages", vehiclesPage.getTotalPages());
	    model.addAttribute("hasNext", vehiclesPage.hasNext());
	    model.addAttribute("hasPrevious", vehiclesPage.hasPrevious());
		
        return "vehicles/index";
	}
	
	private VehicleDTO convertToVehicleDTO(Vehicle vehicle) {
		return modelMapper.map(vehicle, VehicleDTO.class);
	}
}
