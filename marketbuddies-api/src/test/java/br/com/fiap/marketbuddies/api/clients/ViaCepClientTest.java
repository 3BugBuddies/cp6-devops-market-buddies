package br.com.fiap.marketbuddies.api.clients;

import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.exceptions.CepLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepClientTest {

    private static final String URL = "https://viacep.com.br/ws/01310100/json/";

    private MockRestServiceServer server;
    private ViaCepClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ViaCepClient(builder);
    }

    @Test
    void devolveEnderecoParcialComCepNormalizado() {
        server.expect(requestTo(URL)).andExpect(method(GET)).andRespond(withSuccess("""
                {"cep": "01310-100", "logradouro": "Avenida Paulista", "complemento": "de 612 a 1510 - lado par",
                 "unidade": "", "bairro": "Bela Vista", "localidade": "São Paulo", "uf": "SP", "estado": "São Paulo",
                 "ibge": "3550308", "ddd": "11"}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.find("01310100")).contains(
                AddressDto.lookup("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP"));
        server.verify();
    }

    @Test
    void cepInexistenteDevolveVazio() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThat(client.find("01310100")).isEmpty();
    }

    @Test
    void erroComoTextoTambemDevolveVazio() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"erro\": \"true\"}", MediaType.APPLICATION_JSON));

        assertThat(client.find("01310100")).isEmpty();
    }

    @Test
    void timeoutViraExcecaoDeServicoIndisponivel() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.find("01310100"))
                .isInstanceOf(CepLookupException.class)
                .hasMessage("Não foi possível consultar o CEP agora; preencha o endereço à mão.");
    }

    @Test
    void erroDoServicoViraExcecaoDeServicoIndisponivel() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.find("01310100")).isInstanceOf(CepLookupException.class);
    }
}
