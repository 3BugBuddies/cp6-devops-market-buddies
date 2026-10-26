package br.com.fiap.marketbuddies.api.clients;

import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.exceptions.CepLookupException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class ViaCepClient {

    static final String BASE_URL = "https://viacep.com.br/ws";
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    public ViaCepClient() {
        this(RestClient.builder().requestFactory(requestFactory()));
    }

    /** Usado pelos testes com MockRestServiceServer. */
    ViaCepClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * Consulta um CEP já normalizado (8 dígitos). Vazio quando o ViaCEP responde {@code {"erro": true}};
     * {@link CepLookupException} quando o serviço não responde.
     */
    public Optional<AddressDto> find(String cep) {
        ViaCepResponse response;
        try {
            response = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientException ex) {
            log.warn("ViaCEP indisponível para o CEP {}: {}", cep, ex.getMessage());
            throw new CepLookupException(ex);
        }
        if (response == null || response.erro() != null) {
            return Optional.empty();
        }
        return Optional.of(AddressDto.lookup(cep, response.logradouro(), response.bairro(), response.localidade(),
                response.uf()));
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** JSON do ViaCEP; {@code erro} já veio como {@code true} e como {@code "true"}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViaCepResponse(String cep, String logradouro, String bairro, String localidade, String uf,
                                  Object erro) {
    }
}
