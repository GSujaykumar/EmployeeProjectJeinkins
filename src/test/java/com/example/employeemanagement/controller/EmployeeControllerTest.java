package com.example.employeemanagement.controller;

import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.exception.GlobalExceptionHandler;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CONTROLLER TEST (MockMvc) = test HTTP layer only.
 *
 * What loads:   EmployeeController + GlobalExceptionHandler
 * What is fake: EmployeeService (@MockBean)
 *
 * MockMvc simulates HTTP calls like Postman:
 *   POST /api/employees  →  check status 201, JSON body, etc.
 *
 * Difference from Phase 1 (Mockito):
 *   Phase 1 → tested Service methods directly (no HTTP)
 *   Phase 2 → tests URL, status code, JSON response (HTTP)
 */
@WebMvcTest(EmployeeController.class) // load ONLY web controller, not full Spring Boot app
@Import(GlobalExceptionHandler.class)   // needed so 404/400 errors return proper JSON
@AutoConfigureMockMvc(addFilters = false) // disable Spring Security filters for these tests
class EmployeeControllerTest {

    /**
     * MockMvc = fake HTTP client.
     * Sends requests to controller without starting a real server on a port.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper converts Java objects ↔ JSON strings.
     * Example: Employee object → {"firstName":"Asta",...}
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockBean = fake service inside Spring test context.
     * Controller calls this fake service instead of real EmployeeServiceImpl.
     */
    @MockBean
    private EmployeeService employeeService;

    private Employee sampleEmployee;

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
    // POST /api/employees
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/employees - should return 201 Created")
    void createEmployee_success() throws Exception {
        // ARRANGE — when controller calls service, return sample employee
        when(employeeService.createEmployee(any(Employee.class))).thenReturn(sampleEmployee);

        // ACT + ASSERT in one chain using MockMvc
        mockMvc.perform(
                        post("/api/employees") // HTTP POST
                                .contentType(MediaType.APPLICATION_JSON) // request body is JSON
                                .content(objectMapper.writeValueAsString(sampleEmployee)) // body
                )
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.email").value("asta@email.com")) // check JSON field
                .andExpect(jsonPath("$.firstName").value("Asta"));

        verify(employeeService).createEmployee(any(Employee.class));
    }

    @Test
    @DisplayName("POST /api/employees - should return 400 when firstName is blank")
    void createEmployee_validationFails_missingFirstName() throws Exception {
        // ARRANGE — invalid employee (firstName is required @NotBlank)
        Employee invalid = Employee.builder()
                .firstName("") // blank = validation error
                .lastName("Black")
                .email("asta@email.com")
                .department("Engineering")
                .salary(50000.0)
                .build();

        // ACT + ASSERT
        mockMvc.perform(
                        post("/api/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        // service must NOT be called when validation fails before controller logic
        verify(employeeService, never()).createEmployee(any());
    }

    @Test
    @DisplayName("POST /api/employees - should return 400 when email is invalid")
    void createEmployee_validationFails_invalidEmail() throws Exception {
        Employee invalid = Employee.builder()
                .firstName("Asta")
                .lastName("Black")
                .email("not-an-email") // fails @Email validation
                .department("Engineering")
                .salary(50000.0)
                .build();

        mockMvc.perform(
                        post("/api/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        verify(employeeService, never()).createEmployee(any());
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/employees/{id}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/employees/{id} - should return 200 OK")
    void getEmployeeById_success() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleEmployee);

        mockMvc.perform(get("/api/employees/1")) // HTTP GET with path variable id=1
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("asta@email.com"));

        verify(employeeService).getEmployeeById(1L);
    }

    @Test
    @DisplayName("GET /api/employees/{id} - should return 404 when not found")
    void getEmployeeById_notFound() throws Exception {
        // ARRANGE — service throws exception → GlobalExceptionHandler returns 404 JSON
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/employees
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/employees - should return 200 and list")
    void getAllEmployees_success() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(sampleEmployee));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("asta@email.com")) // first item in array
                .andExpect(jsonPath("$[0].firstName").value("Asta"));

        verify(employeeService).getAllEmployees();
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /api/employees/{id}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/employees/{id} - should return 200 OK")
    void updateEmployee_success() throws Exception {
        Employee updateRequest = Employee.builder()
                .firstName("Yuno")
                .lastName("Black")
                .email("yuno@email.com")
                .department("HR")
                .salary(60000.0)
                .build();

        Employee updated = Employee.builder()
                .id(1L)
                .firstName("Yuno")
                .lastName("Black")
                .email("yuno@email.com")
                .department("HR")
                .salary(60000.0)
                .build();

        when(employeeService.updateEmployee(eq(1L), any(Employee.class))).thenReturn(updated);

        mockMvc.perform(
                        put("/api/employees/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Yuno"))
                .andExpect(jsonPath("$.department").value("HR"));

        verify(employeeService).updateEmployee(eq(1L), any(Employee.class));
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE /api/employees/{id}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /api/employees/{id} - should return 200 OK")
    void deleteEmployee_success() throws Exception {
        // deleteEmployee in controller returns void from service — just verify call
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Employee deleted successfully"));

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - should return 404 when not found")
    void deleteEmployee_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
