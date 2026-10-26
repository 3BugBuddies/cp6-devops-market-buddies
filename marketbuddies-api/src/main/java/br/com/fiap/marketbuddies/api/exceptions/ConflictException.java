package br.com.fiap.marketbuddies.api.exceptions;

import lombok.Getter;

/** Conflito com um registro existente (409). {@code field} é opcional: quando vem, o erro aparece no campo. */
@Getter
public class ConflictException extends RuntimeException {

    private final String field;

    public ConflictException(String message) {
        this(null, message);
    }

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }
}
