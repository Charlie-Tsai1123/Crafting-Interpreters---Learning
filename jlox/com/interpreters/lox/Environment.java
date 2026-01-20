package com.interpreters.lox;
import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> environment = new HashMap<>();

    void define(String name, Object value) {
        environment.put(name, value);
    }

    Object get(Token name) {
        if (environment.containsKey(name.lexeme)) {
            return environment.get(name.lexeme);
        }
        throw new RuntimeError(name, "Undefined variable " + name.lexeme + ".");
    }

    void assign(Token name, Object value) {
        if (environment.containsKey(name.lexeme)) {
            environment.put(name.lexeme, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
