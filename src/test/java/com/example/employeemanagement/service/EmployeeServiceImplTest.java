package com.example.employeemanagement.service;

import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST = test ONE class in isolation (EmployeeServiceImpl).
 * We do NOT start Spring Boot and we do NOT use a real database.
 *
 * Instead we use a FAKE repository (@Mock) and Mockito controls what it returns.
 */
@ExtendWith(MockitoExtension.class) // tells JUnit to activate Mockito for this class
class EmployeeServiceImplTest {

    /**
     * @Mock = fake EmployeeRepository.
     * When service calls repository methods, WE decide the fake answer using when(...).
     */
    @Mock
    private EmployeeRepository employeeRepository;

    /**
     * @InjectMocks = real EmployeeServiceImpl object.
     * Mockito automatically injects the fake repository above into this service.
     */
    @InjectMocks
    private EmployeeServiceImpl employeeService;

    // Reusable test data — created fresh before EVERY test method
    private Employee sampleEmployee;

    /**
     * @BeforeEach runs before each @Test.
     * So every test starts with the same clean sample employee object.
     */
    @BeforeEach
    void setUp() {
        sampleEmployee = Employee.builder()
                .id(1L)
                .firstName("Asta")
                .lastName("Black")
                .email("asta@email.com")
                .department("Engineering")
                .salary(50000.0)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // CREATE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("createEmployee - should save when email is unique")
    void createEmployee_success() {
        // ARRANGE (Given) — set up fake repository behaviour
        // "When service checks email, pretend no employee exists"
        when(employeeRepository.findByEmail("asta@email.com")).thenReturn(Optional.empty());
        // "When service saves, return our sample employee"
        when(employeeRepository.save(any(Employee.class))).thenReturn(sampleEmployee);

        // ACT (When) — call the real service method
        Employee result = employeeService.createEmployee(sampleEmployee);

        // ASSERT (Then) — check the result is correct
        assertNotNull(result); // result should not be null
        assertEquals("asta@email.com", result.getEmail()); // email should match

        // VERIFY — confirm repository methods were actually called
        verify(employeeRepository).findByEmail("asta@email.com"); // email check happened
        verify(employeeRepository).save(sampleEmployee); // save happened
    }

    @Test
    @DisplayName("createEmployee - should throw when email already exists")
    void createEmployee_duplicateEmail_throwsException() {
        // ARRANGE — pretend email already exists in DB
        Employee existingEmployee = Employee.builder()
                .id(2L)
                .email("asta@email.com")
                .build();
        when(employeeRepository.findByEmail("asta@email.com"))
                .thenReturn(Optional.of(existingEmployee));

        // ACT + ASSERT — service should throw IllegalArgumentException
        // assertThrows runs the lambda and expects an exception
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.createEmployee(sampleEmployee)
        );
        assertEquals("Employee email already exists", ex.getMessage());

        // save() must NEVER be called when email is duplicate
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ═══════════════════════════════════════════════════════════
    // READ TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getEmployeeById - should return employee when found")
    void getEmployeeById_found() {
        // ARRANGE — fake DB has employee with id=1
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        // ACT
        Employee result = employeeService.getEmployeeById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Asta", result.getFirstName());
        verify(employeeRepository).findById(1L);
    }

    @Test
    @DisplayName("getEmployeeById - should throw when employee not found")
    void getEmployeeById_notFound() {
        // ARRANGE — fake DB returns empty = no employee with id 99
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT — service throws ResourceNotFoundException
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.getEmployeeById(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("getAllEmployees - should return list of employees")
    void getAllEmployees_returnsList() {
        // ARRANGE — fake DB returns a list with 1 employee
        when(employeeRepository.findAll()).thenReturn(List.of(sampleEmployee));

        // ACT
        List<Employee> result = employeeService.getAllEmployees();

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("asta@email.com", result.get(0).getEmail());
        verify(employeeRepository).findAll();
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateEmployee - should update employee when found")
    void updateEmployee_success() {
        // ARRANGE — employee exists in fake DB
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        // updated data from API request
        Employee updateRequest = Employee.builder()
                .firstName("Yuno")
                .lastName("Black")
                .email("yuno@email.com")
                .department("HR")
                .salary(60000.0)
                .build();

        // when save is called, return employee with updated fields
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0); // get object passed to save()
            return saved;
        });

        // ACT
        Employee result = employeeService.updateEmployee(1L, updateRequest);

        // ASSERT — fields should be updated
        assertEquals("Yuno", result.getFirstName());
        assertEquals("yuno@email.com", result.getEmail());
        assertEquals("HR", result.getDepartment());
        assertEquals(60000.0, result.getSalary());
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("updateEmployee - should throw when employee not found")
    void updateEmployee_notFound() {
        // ARRANGE — no employee with id 99
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Employee updateRequest = Employee.builder()
                .firstName("Yuno")
                .lastName("Black")
                .email("yuno@email.com")
                .department("HR")
                .salary(60000.0)
                .build();

        // ACT + ASSERT
        assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.updateEmployee(99L, updateRequest)
        );

        // update must not try to save when employee doesn't exist
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteEmployee - should delete when employee exists")
    void deleteEmployee_success() {
        // ARRANGE — employee exists
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        // ACT — void method, no return value to check
        employeeService.deleteEmployee(1L);

        // ASSERT using verify — delete() must be called once with our employee
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).delete(sampleEmployee);
    }

    @Test
    @DisplayName("deleteEmployee - should throw when employee not found")
    void deleteEmployee_notFound() {
        // ARRANGE
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.deleteEmployee(99L)
        );

        // delete() must not run if employee was not found
        verify(employeeRepository, never()).delete(any(Employee.class));
    }
}
