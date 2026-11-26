package com.sandovalpurisaca.almuerzoservice;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AlmuerzoApiIntegrationTests {

    @LocalServerPort
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ObjectMapper mapeador = JsonMapper.builder().build();

    @Test
    void flujoCompletoConTokenYAislamientoEntreUsuarios() throws Exception {
        String base = "http://localhost:" + puerto + "/api";

        // Sin token, la lista responde 401.
        assertEquals(401, enviar(get(base + "/almuerzos", null)).statusCode());

        // Registro y login devuelven un JWT con tres partes.
        HttpResponse<String> registro = enviar(json(base + "/auth/register",
                "{\"nombre\":\"Ana\",\"email\":\"ana@test.com\",\"password\":\"almuerzo123\"}", null, "POST"));
        assertEquals(200, registro.statusCode());
        JsonNode datosRegistro = mapeador.readTree(registro.body());
        String token = datosRegistro.get("token").asText();
        assertEquals(3, token.split("\\.").length);
        assertEquals("Bearer", datosRegistro.get("tipo").asText());

        HttpResponse<String> login = enviar(json(base + "/auth/login",
                "{\"email\":\"ana@test.com\",\"password\":\"almuerzo123\"}", null, "POST"));
        assertEquals(200, login.statusCode());

        HttpResponse<String> loginInvalido = enviar(json(base + "/auth/login",
                "{\"email\":\"ana@test.com\",\"password\":\"incorrecta\"}", null, "POST"));
        assertEquals(401, loginInvalido.statusCode());

        // Un token alterado también responde 401.
        HttpResponse<String> alterado = enviar(get(base + "/almuerzos", token + "x"));
        assertEquals(401, alterado.statusCode(),
                "Token alterado devolvió " + alterado.statusCode() + ": " + alterado.body());

        // Crear pedido: el total se calcula en la API.
        HttpResponse<String> creado = enviar(json(base + "/almuerzos",
                "{\"cliente\":\"Carlos\",\"plato\":\"Arroz con pato\",\"cantidad\":2,\"precio\":25.50}",
                token, "POST"));
        assertEquals(201, creado.statusCode());
        JsonNode pedido = mapeador.readTree(creado.body());
        long id = pedido.get("id").asLong();
        assertEquals(51.0, pedido.get("total").asDouble());
        assertFalse(pedido.get("entregado").asBoolean());
        assertTrue(pedido.has("fechaPedido"));

        // Listar, obtener y resumen con token.
        HttpResponse<String> lista = enviar(get(base + "/almuerzos", token));
        assertEquals(200, lista.statusCode());
        assertEquals(id, mapeador.readTree(lista.body()).get(0).get("id").asLong());

        assertEquals(200, enviar(get(base + "/almuerzos/" + id, token)).statusCode());

        HttpResponse<String> resumen = enviar(get(base + "/almuerzos/resumen", token));
        assertEquals(200, resumen.statusCode());
        assertEquals(51.0, mapeador.readTree(resumen.body()).get("montoTotal").asDouble());

        // Otro usuario no puede ver ni modificar el pedido del primero.
        HttpResponse<String> registro2 = enviar(json(base + "/auth/register",
                "{\"nombre\":\"Beto\",\"email\":\"beto@test.com\",\"password\":\"almuerzo123\"}", null, "POST"));
        String token2 = mapeador.readTree(registro2.body()).get("token").asText();
        assertEquals(404, enviar(get(base + "/almuerzos/" + id, token2)).statusCode());
        assertEquals(0, mapeador.readTree(enviar(get(base + "/almuerzos", token2)).body()).size());

        // Actualizar (marcar entregado), anular y verificar que desaparece.
        HttpResponse<String> actualizado = enviar(json(base + "/almuerzos/" + id,
                "{\"cliente\":\"Carlos\",\"plato\":\"Arroz con pato\",\"cantidad\":2,\"precio\":25.50,\"entregado\":true}",
                token, "PUT"));
        assertEquals(200, actualizado.statusCode());
        assertTrue(mapeador.readTree(actualizado.body()).get("entregado").asBoolean());

        HttpResponse<String> anulado = enviar(HttpRequest.newBuilder(URI.create(base + "/almuerzos/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE().build());
        assertEquals(204, anulado.statusCode());
        assertEquals(0, mapeador.readTree(enviar(get(base + "/almuerzos", token)).body()).size());
    }

    private HttpRequest get(String url, String token) {
        var constructor = HttpRequest.newBuilder(URI.create(url)).GET();
        if (token != null) {
            constructor.header("Authorization", "Bearer " + token);
        }
        return constructor.build();
    }

    private HttpRequest json(String url, String cuerpo, String token, String metodo) {
        var constructor = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) {
            constructor.header("Authorization", "Bearer " + token);
        }
        return constructor.build();
    }

    private HttpResponse<String> enviar(HttpRequest peticion) throws Exception {
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }
}
