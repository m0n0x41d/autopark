package ru.krivonogova.autopark.models;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "brand")
public class Brand {
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "type_vehicle")
	private TypeVehicle typeVehicle;
	
	@Column(name = "tank_capacity")
	private double tankCapacity;
	
	@Column(name = "load_capacity")
	private double loadCapacity;
	
	@Column(name = "passenger_capacity")
	private int passengerCapacity;
	
	@Column(name = "engine_power")
	private double enginePower;
	
	@OneToMany(mappedBy = "brand")
	private List<Vehicle> vehicles;

	public Brand() {
	}

	public Brand(int id, TypeVehicle typeVehicle, double tankCapacity, double loadCapacity, int passengerCapacity,
			double enginePower) {
		this.id = id;
		this.typeVehicle = typeVehicle;
		this.tankCapacity = tankCapacity;
		this.loadCapacity = loadCapacity;
		this.passengerCapacity = passengerCapacity;
		this.enginePower = enginePower;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public TypeVehicle getTypeVehicle() {
		return typeVehicle;
	}

	public void setTypeVehicle(TypeVehicle typeVehicle) {
		this.typeVehicle = typeVehicle;
	}

	public double getTankCapacity() {
		return tankCapacity;
	}

	public void setTankCapacity(double tankCapacity) {
		this.tankCapacity = tankCapacity;
	}

	public double getLoadCapacity() {
		return loadCapacity;
	}

	public void setLoadCapacity(double loadCapacity) {
		this.loadCapacity = loadCapacity;
	}

	public int getPassengerCapacity() {
		return passengerCapacity;
	}

	public void setPassengerCapacity(int passengerCapacity) {
		this.passengerCapacity = passengerCapacity;
	}

	public double getEnginePower() {
		return enginePower;
	}

	public void setEnginePower(double enginePower) {
		this.enginePower = enginePower;
	}

	public List<Vehicle> getVehicles() {
		return vehicles;
	}

	public void setVehicles(List<Vehicle> vehicles) {
		this.vehicles = vehicles;
	}
	
}