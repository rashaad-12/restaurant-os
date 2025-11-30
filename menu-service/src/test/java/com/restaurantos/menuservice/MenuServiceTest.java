package com.restaurantos.menuservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.restaurantos.common.util.JsonUtil;
import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.service.MenuService;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MenuServiceTest extends AbstractIntegrationTest {

    @Autowired
    MenuService menuService;

    public static Stream<Arguments> createMenuTestData() {
        return Stream.of(
            Arguments.of("standard single menu", "test-data/create/single/"),
            Arguments.of("standard multiple menu", "test-data/create/multiple/"),
            Arguments.of("without status", "test-data/create/without-status/")
        );
    }

    @MethodSource("createMenuTestData")
    @ParameterizedTest(name = "{0}")
    void createMenuTest(String testName, String testDataPath) throws JSONException {

        List<MenuDTO> request = JsonUtil.readFromFile(testDataPath + "request.json", new TypeReference<>() {});
        String createMenuResponse = menuService.createMenu(request);

        assertEquals("Menu creation request was processed successfully", createMenuResponse);

        List<MenuDTO> getMenuResponse = menuService.getMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);

        List<MenuDTO> expected = JsonUtil.readFromFile(testDataPath + "response.json", new TypeReference<>() {});
        String expectedResponse = JsonUtil.writeValueAsString(expected);
        String actualResponse = JsonUtil.writeValueAsString(getMenuResponse);

        JSONAssert.assertEquals(expectedResponse, actualResponse, false);
    }

    public static Stream<Arguments> updateMenuTestData() {
        return Stream.of(
            Arguments.of("update single", "test-data/update/single/"),
            Arguments.of("update multiple", "test-data/update/multiple/"),
            Arguments.of("add menuItem", "test-data/update/add-menuItem/"),
            Arguments.of("update menuItem", "test-data/update/update-menuItem/"),
            Arguments.of("delete menuItem", "test-data/update/delete-menuItem/"),
            Arguments.of("with combos", "test-data/update/with-combos/"),
            Arguments.of("without combos", "test-data/update/without-combos/"),
            Arguments.of("with variants", "test-data/update/with-variants/"),
            Arguments.of("without variants", "test-data/update/without-variants/")
        );
    }

    @MethodSource("updateMenuTestData")
    @ParameterizedTest(name = "{0}")
    void updateMenuTest(String testName, String testDataPath) throws JSONException {

        List<MenuDTO> createRequest = JsonUtil.readFromFile(testDataPath + "create.json", new TypeReference<>() {});
        String createMenuResponse = menuService.createMenu(createRequest);

        assertEquals("Menu creation request was processed successfully", createMenuResponse);

        List<MenuDTO> getMenuResponse = menuService.getMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);

        List<MenuDTO> updateRequest = JsonUtil.readFromFile(testDataPath + "update.json", new TypeReference<>() {});
        String updateMenuResponse = menuService.updateMenu(updateRequest);

        assertEquals("Menu updation request was processed successfully", updateMenuResponse);

        getMenuResponse = menuService.getMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);

        List<MenuDTO> expected = JsonUtil.readFromFile(testDataPath + "response.json", new TypeReference<>() {});
        String expectedResponse = JsonUtil.writeValueAsString(expected);
        String actualResponse = JsonUtil.writeValueAsString(getMenuResponse);

        JSONAssert.assertEquals(expectedResponse, actualResponse, false);
    }

    @Test
    void menuServiceWorkflowTest() {

        List<MenuDTO> createRequest = JsonUtil.readFromFile("test-data/workflow/create.json", new TypeReference<>() {});
        String createMenuResponse = menuService.createMenu(createRequest);

        assertEquals("Menu creation request was processed successfully", createMenuResponse);

        List<MenuDTO> getMenuResponse = menuService.getMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);
        assertEquals(3, getMenuResponse.size());

        getMenuResponse = menuService.getPublishedMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);
        assertEquals(1, getMenuResponse.size());

        List<MenuDTO> updateRequest = JsonUtil.readFromFile("test-data/workflow/publish.json", new TypeReference<>() {});
        String publishMenu = menuService.publishMenu(updateRequest);

        assertEquals("Menu publish request was processed successfully", publishMenu);

        getMenuResponse = menuService.getPublishedMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);
        assertEquals(2, getMenuResponse.size());

        MenuDTO getMenuDetailsResponse = menuService.getMenuByCodeAndRestaurantCode("LMMU-002", "ZZZZ");
        assertNotNull(getMenuDetailsResponse);
        assertEquals("LMMU-002", getMenuDetailsResponse.getCode());
        assertEquals("PUBLISHED", getMenuDetailsResponse.getStatus().name());
        assertNotNull(getMenuDetailsResponse.getCreateDttm());
        assertNotNull(getMenuDetailsResponse.getUpdateDttm());
        assertNotNull(getMenuDetailsResponse.getPublishDttm());

        List<MenuDTO> archiveRequest = JsonUtil.readFromFile("test-data/workflow/archive.json", new TypeReference<>() {});
        String archiveMenu = menuService.archiveMenu(archiveRequest);

        assertEquals("Menu archive request was processed successfully", archiveMenu);

        getMenuResponse = menuService.getPublishedMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);
        assertEquals(0, getMenuResponse.size());

        List<MenuDTO> deleteRequest = JsonUtil.readFromFile("test-data/workflow/delete.json", new TypeReference<>() {});
        String deleteMenu = menuService.deleteMenu(deleteRequest);

        assertEquals("Menu deletion request was processed successfully", deleteMenu);

        getMenuResponse = menuService.getMenuByRestaurant(Collections.singleton("ZZZZ"));
        assertNotNull(getMenuResponse);
        assertEquals(0, getMenuResponse.size());
    }

}