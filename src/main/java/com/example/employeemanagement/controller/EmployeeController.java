package com.example.employeemanagement.controller;

import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(
            summary = "Create Employee",
            description = "Creates a new employee and saves into database"
    )
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        log.info("Received request to create employee");
        return new ResponseEntity<>(employeeService.createEmployee(employee), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")

    @Operation(summary = "Get Employee By Id",
    description = "Get Employees By Id")
    public ResponseEntity<Employee> getEmployeeById(
            @Parameter (description = "EmployeeId")
            @PathVariable Long id) {
        log.info("Received request to fetch employee by id: {}", id);
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping
    @Operation(summary = "Get All Employees",
    description = "Get all Employees from Database")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.info("Received request to fetch all employees");
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PutMapping("/{id}")
    @Operation (summary = "UPdate Employee By id",
    description = "UPdates employees Into Database")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id,
                                                   @Valid @RequestBody Employee employee) {
        log.info("Received request to update employee with id: {}", id);
        return ResponseEntity.ok(employeeService.updateEmployee(id, employee));
    }

    @DeleteMapping("/{id}")
    @Operation (summary = "delete Employee By id",
            description = "Delete employees from Database")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        log.info("Received request to delete employee with id: {}", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deleted successfully");
    }
}