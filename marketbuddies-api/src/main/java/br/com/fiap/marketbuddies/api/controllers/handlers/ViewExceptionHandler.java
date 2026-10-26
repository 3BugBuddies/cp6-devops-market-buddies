package br.com.fiap.marketbuddies.api.controllers.handlers;

import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.exceptions.OutOfStockException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ViewExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException ex) {
        return page("error/404", HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({OutOfStockException.class, ConflictException.class, BusinessException.class})
    public ModelAndView handleBusiness(RuntimeException ex) {
        HttpStatus status = ex instanceof BusinessException ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.CONFLICT;
        return page("error", status, ex.getMessage());
    }

    private ModelAndView page(String view, HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(view, status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        return mav;
    }
}
