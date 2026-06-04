package com.example.employeemanagement.service.impl;

import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public Employee createEmployee(Employee employee) {
        log.info("Creating employee with email: {}", employee.getEmail());

        employeeRepository.findByEmail(employee.getEmail())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Employee email already exists");
                });

        return employeeRepository.save(employee);
    }

    @Override
    @Cacheable(value = "EmployeeById" ,key = "#id")
    public Employee getEmployeeById(Long id) {
        log.info("Fetching employee by id: {}", id);

        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    @Override
    @Cacheable(value = "Employees" )
    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees");
        return employeeRepository.findAll();
    }

    @Override
    @CachePut(value = "Employees",key="'all'")
    public Employee updateEmployee(Long id, Employee employee) {
        log.info("Updating employee with id: {}", id);

        Employee existingEmployee = getEmployeeById(id);

        existingEmployee.setFirstName(employee.getFirstName());
        existingEmployee.setLastName(employee.getLastName());
        existingEmployee.setEmail(employee.getEmail());
        existingEmployee.setDepartment(employee.getDepartment());
        existingEmployee.setSalary(employee.getSalary());

        return employeeRepository.save(existingEmployee);
    }

    @Override
    @CacheEvict(value = "Employees",key="'all'")
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with id: {}", id);

        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }
}