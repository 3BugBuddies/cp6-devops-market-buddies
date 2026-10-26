package br.com.fiap.marketbuddies.api.exceptions;

/** O ViaCEP não respondeu (rede, timeout ou erro do serviço). Vira 502. */
public class CepLookupException extends RuntimeException {

    public static final String MESSAGE = "Não foi possível consultar o CEP agora; preencha o endereço à mão.";

    public CepLookupException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
