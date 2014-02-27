package com.bia.services;

import com.bia.domain.Employee;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

	public long countAllEmployees() {
        return Employee.countEmployees();
    }

	public void deleteEmployee(Employee employee) {
        employee.remove();
    }

	public Employee findEmployee(Long id) {
        return Employee.findEmployee(id);
    }

	public List<Employee> findAllEmployees() {
        return Employee.findAllEmployees();
    }

	public List<Employee> findEmployeeEntries(int firstResult, int maxResults) {
        return Employee.findEmployeeEntries(firstResult, maxResults);
    }

	public void saveEmployee(Employee employee) {
        employee.persist();
    }

	public Employee updateEmployee(Employee employee) {
        return employee.merge();
    }
	
	public List<Employee> search(String queryString) {
		return Employee.search(queryString);
	}
	
	public void deleteAll() {
		Employee.deleteAll();
	}
}
