package am.techshop.order.controller;

import am.techshop.common.dto.request.AddRepairEntryRequest;
import am.techshop.common.dto.request.UpdateNotesRequest;
import am.techshop.common.dto.response.DigitalTwinDetailResponse;
import am.techshop.common.dto.response.DigitalTwinSummaryResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.config.SecurityConfig;
import am.techshop.order.security.JwtAuthFilter;
import am.techshop.order.security.JwtService;
import am.techshop.order.service.DigitalTwinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DigitalTwinController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class DigitalTwinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DigitalTwinService digitalTwinService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void getMyDigitalTwins_ReturnsUsersDigitalTwins() throws Exception {
        DigitalTwinSummaryResponse summary = new DigitalTwinSummaryResponse(
                1L, "Phone", LocalDate.now(), LocalDate.now().plusYears(2), true);
        when(digitalTwinService.getMyDigitalTwins(USER_ID)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/orders/my-products").with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productName").value("Phone"))
                .andExpect(jsonPath("$.data[0].warrantyActive").value(true));
    }

    @Test
    void getMyDigitalTwins_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/my-products"))
                .andExpect(status().isUnauthorized());

        verify(digitalTwinService, never()).getMyDigitalTwins(any());
    }

    @Test
    void getDigitalTwinDetail_WhenOwned_ReturnsDetail() throws Exception {
        DigitalTwinDetailResponse detail = new DigitalTwinDetailResponse(
                1L, "Phone", 100L, LocalDate.now(), LocalDate.now().plusYears(2), true, "notes", List.of());
        when(digitalTwinService.getDigitalTwinDetail(USER_ID, 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/orders/my-products/{id}", 1L).with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Phone"))
                .andExpect(jsonPath("$.data.notes").value("notes"));
    }

    @Test
    void getDigitalTwinDetail_WhenNotOwnedByCaller_ReturnsNotFound() throws Exception {
        when(digitalTwinService.getDigitalTwinDetail(USER_ID, 1L))
                .thenThrow(new TechShopException("Digital twin not found", 404));

        mockMvc.perform(get("/api/orders/my-products/{id}", 1L).with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void addRepairEntry_WhenOwned_ReturnsUpdatedDetail() throws Exception {
        AddRepairEntryRequest request = new AddRepairEntryRequest("Screen replaced", LocalDate.now());
        DigitalTwinDetailResponse detail = new DigitalTwinDetailResponse(
                1L, "Phone", 100L, LocalDate.now(), LocalDate.now().plusYears(2), true, null, List.of());
        when(digitalTwinService.addRepairEntry(eq(USER_ID), eq(1L), any())).thenReturn(detail);

        mockMvc.perform(post("/api/orders/my-products/{id}/repairs", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repair entry added"));
    }

    @Test
    void addRepairEntry_WhenNotOwnedByCaller_ReturnsNotFound() throws Exception {
        AddRepairEntryRequest request = new AddRepairEntryRequest("Screen replaced", LocalDate.now());
        when(digitalTwinService.addRepairEntry(eq(USER_ID), eq(1L), any()))
                .thenThrow(new TechShopException("Digital twin not found", 404));

        mockMvc.perform(post("/api/orders/my-products/{id}/repairs", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addRepairEntry_WithBlankDescription_ReturnsBadRequest() throws Exception {
        AddRepairEntryRequest request = new AddRepairEntryRequest("", LocalDate.now());

        mockMvc.perform(post("/api/orders/my-products/{id}/repairs", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(digitalTwinService, never()).addRepairEntry(any(), any(), any());
    }

    @Test
    void updateNotes_WhenOwned_ReturnsUpdatedDetail() throws Exception {
        UpdateNotesRequest request = new UpdateNotesRequest("Gift for dad");
        DigitalTwinDetailResponse detail = new DigitalTwinDetailResponse(
                1L, "Phone", 100L, LocalDate.now(), LocalDate.now().plusYears(2), true, "Gift for dad", List.of());
        when(digitalTwinService.updateNotes(eq(USER_ID), eq(1L), any())).thenReturn(detail);

        mockMvc.perform(put("/api/orders/my-products/{id}/notes", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("Gift for dad"));
    }

    @Test
    void updateNotes_WhenNotOwnedByCaller_ReturnsNotFound() throws Exception {
        UpdateNotesRequest request = new UpdateNotesRequest("hijacked");
        when(digitalTwinService.updateNotes(eq(USER_ID), eq(1L), any()))
                .thenThrow(new TechShopException("Digital twin not found", 404));

        mockMvc.perform(put("/api/orders/my-products/{id}/notes", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
