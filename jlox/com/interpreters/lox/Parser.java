package com.interpreters.lox;

import java.util.ArrayList;
import java.util.List;
import static com.interpreters.lox.TokenType.*;

/*
Expression

expression     → comma ;
comma          → equality ( "," equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
*/

/*
Statement

program        → statement* EOF ;

statement      → exprStmt
               | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
*/

class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private static class ParseError extends RuntimeException {};

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        // try {
        //     return expression();
        // } catch (ParseError error) {
        //     return null;
        // }
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }

        return statements;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Expr expression() {
        // expression     → equality ;
        return comma();
    }

    private Expr comma() {
        // comma       → equality ( "," equality )* ;
        Expr expr = equality();
        while (match(COMMA)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        // equality       → comparison ( ( "!=" | "==" ) comparison )* ;

        // without left operand
        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Lox.error(operator, "Expect left expression");
            comparison();
            return new Expr.Literal(null);
        }

        // correct
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        // without left operand
        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Lox.error(operator, "Expect left expression.");
            term();
            return new Expr.Literal(null);
        }

        // correct
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        // term           → factor ( ( "-" | "+" ) factor )* ;
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        // factor         → unary ( ( "/" | "*" ) unary )* ;
        // without left operand
        if (match(SLASH, STAR)) {
            Token operator = previous();
            Lox.error(operator, "Expect left operand.");
            unary();
            return new Expr.Literal(null);
        }
        
        // correct
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        // unary          → ( "!" | "-" ) unary
        //                  | primary ;
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr expr = unary();
            return expr = new Expr.Unary(operator, expr);
        }
        return primary();
    }
    
    private Expr primary() {
        // primary        → NUMBER | STRING | "true" | "false" | "nil"
        //                  | "(" expression ")" ;
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) 
            return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case FOR:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
                default:
                    break;
            }
            advance();
        }
    }
    
    private Stmt statement() {
        // statement      → exprStmt
        //                | printStmt ;
        if (match(PRINT)) return printStatement();
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expression);
    }
}
