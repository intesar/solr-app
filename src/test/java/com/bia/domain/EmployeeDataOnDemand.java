package com.bia.domain;
import com.bia.services.EmployeeService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.roo.addon.dod.RooDataOnDemand;
import org.springframework.stereotype.Component;

@Component
@Configurable
@RooDataOnDemand(entity = Employee.class)
public class EmployeeDataOnDemand {

	private Random rnd = new SecureRandom();

	private List<Employee> data;

	@Autowired
    EmployeeService employeeService;

	public Employee getNewTransientEmployee(int index) {
        Employee obj = new Employee();
        setDepartment(obj, index);
        setDisplayName(obj, index);
        setUserName(obj, index);
        return obj;
    }

	public void setDepartment(Employee obj, int index) {
        String department = "department_" + index;
        obj.setDepartment(department);
    }

	public void setDisplayName(Employee obj, int index) {
        String displayName = "intesar Mohammed" + index;
        obj.setDisplayName(displayName);
    }

	public void setUserName(Employee obj, int index) {
        String userName = "userName_" + index;
        if (userName.length() > 30) {
            userName = userName.substring(0, 30);
        }
        obj.setUserName(userName);
    }

	public Employee getSpecificEmployee(int index) {
        init();
        if (index < 0) {
            index = 0;
        }
        if (index > (data.size() - 1)) {
            index = data.size() - 1;
        }
        Employee obj = data.get(index);
        Long id = obj.getId();
        return employeeService.findEmployee(id);
    }

	public Employee getRandomEmployee() {
        init();
        Employee obj = data.get(rnd.nextInt(data.size()));
        Long id = obj.getId();
        return employeeService.findEmployee(id);
    }

	public boolean modifyEmployee(Employee obj) {
        return false;
    }

	public void init() {
		// delete everything
		Employee.deleteAll();
		
        int from = 0;
        int to = 10;
        data = employeeService.findEmployeeEntries(from, to);
        if (data == null) {
            throw new IllegalStateException("Find entries implementation for 'Employee' illegally returned null");
        }
        if (!data.isEmpty()) {
            return;
        }
        
        data = new ArrayList<Employee>();
        for (int i = 0; i < 10; i++) {
            Employee obj = getNewTransientEmployee(i);
            try {
                employeeService.saveEmployee(obj);
            } catch (ConstraintViolationException e) {
                StringBuilder msg = new StringBuilder();
                for (Iterator<ConstraintViolation<?>> iter = e.getConstraintViolations().iterator(); iter.hasNext();) {
                    ConstraintViolation<?> cv = iter.next();
                    msg.append("[").append(cv.getConstraintDescriptor()).append(":").append(cv.getMessage()).append("=").append(cv.getInvalidValue()).append("]");
                }
                throw new RuntimeException(msg.toString(), e);
            }
            obj.flush();
            data.add(obj);
        }
    }
}
