package com.bia.services;
import com.bia.domain.Employee;
import java.util.List;
import org.springframework.roo.addon.layers.service.RooService;

@RooService(domainTypes = { com.bia.domain.Employee.class })
public interface EmployeeService {

	public abstract long countAllEmployees();


	public abstract void deleteEmployee(Employee employee);


	public abstract Employee findEmployee(Long id);


	public abstract List<Employee> findAllEmployees();


	public abstract List<Employee> findEmployeeEntries(int firstResult, int maxResults);


	public abstract void saveEmployee(Employee employee);


	public abstract Employee updateEmployee(Employee employee);
	
	public abstract List<Employee> search(String queryString);
	
	public abstract void deleteAll();

}
