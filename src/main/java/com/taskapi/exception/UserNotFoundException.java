package com.taskapi.exception;

// Exceções customizadas são um sinal de código maduro.
// Evite lançar RuntimeException ou Exception diretamente.

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
