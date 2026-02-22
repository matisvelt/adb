package com.antlab.rigcontrol.sorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RulesEngine {
    private final List<CompiledRule> compiled = new ArrayList<>();

    public void setRules(List<Rule> rules) {
        compiled.clear();
        if (rules == null) {
            return;
        }
        for (Rule rule : rules) {
            if (rule == null) {
                continue;
            }
            Expression expr = null;
            if (rule.getCondition() != null && !rule.getCondition().isBlank()) {
                expr = new Parser(rule.getCondition()).parse();
            }
            compiled.add(new CompiledRule(rule, expr));
        }
    }

    public RuleMatch evaluate(FileRecord record) {
        for (CompiledRule compiledRule : compiled) {
            Rule rule = compiledRule.rule;
            if (rule == null || !rule.isEnabled()) {
                continue;
            }
            if (compiledRule.expression == null) {
                continue;
            }
            if (compiledRule.expression.evalBool(record)) {
                return new RuleMatch(rule, rule.getDestination());
            }
        }
        return null;
    }

    public String validateCondition(String condition) {
        try {
            new Parser(condition).parse();
            return null;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    public static class RuleMatch {
        private final Rule rule;
        private final String destination;

        public RuleMatch(Rule rule, String destination) {
            this.rule = rule;
            this.destination = destination;
        }

        public Rule getRule() {
            return rule;
        }

        public String getDestination() {
            return destination;
        }
    }

    private static class CompiledRule {
        private final Rule rule;
        private final Expression expression;

        private CompiledRule(Rule rule, Expression expression) {
            this.rule = rule;
            this.expression = expression;
        }
    }

    private interface Expression {
        Object eval(FileRecord record);

        default boolean evalBool(FileRecord record) {
            Object value = eval(record);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return false;
        }
    }

    private static class LiteralExpression implements Expression {
        private final Object value;

        private LiteralExpression(Object value) {
            this.value = value;
        }

        @Override
        public Object eval(FileRecord record) {
            return value;
        }
    }

    private static class IdentifierExpression implements Expression {
        private final String name;

        private IdentifierExpression(String name) {
            this.name = name;
        }

        @Override
        public Object eval(FileRecord record) {
            switch (name) {
                case "label":
                    return record.getLabel();
                case "confidence":
                    return record.getConfidence();
                case "facesCount":
                    return (double) record.getFacesCount();
                case "hasTextLikelihood":
                    return record.getHasTextLikelihood();
                case "isDocumentLikelihood":
                    return record.getIsDocumentLikelihood();
                case "screenshotLikelihood":
                    return record.getScreenshotLikelihood();
                case "extension":
                    return record.getExtension();
                case "fileType":
                    return record.getFileType() == null ? null : record.getFileType().name();
                case "status":
                    return record.getStatus() == null ? null : record.getStatus().name();
                default:
                    return name;
            }
        }
    }

    private static class UnaryExpression implements Expression {
        private final String op;
        private final Expression right;

        private UnaryExpression(String op, Expression right) {
            this.op = op;
            this.right = right;
        }

        @Override
        public Object eval(FileRecord record) {
            Object val = right.eval(record);
            if ("!".equals(op)) {
                return !toBool(val);
            }
            return false;
        }
    }

    private static class BinaryExpression implements Expression {
        private final Expression left;
        private final String op;
        private final Expression right;

        private BinaryExpression(Expression left, String op, Expression right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public Object eval(FileRecord record) {
            if ("&&".equals(op)) {
                return toBool(left.eval(record)) && toBool(right.eval(record));
            }
            if ("||".equals(op)) {
                return toBool(left.eval(record)) || toBool(right.eval(record));
            }

            Object l = left.eval(record);
            Object r = right.eval(record);

            if ("startsWith".equals(op)) {
                String ls = stringValue(l);
                String rs = stringValue(r);
                if (ls == null || rs == null) {
                    return false;
                }
                return ls.startsWith(rs);
            }

            Double ln = toNumber(l);
            Double rn = toNumber(r);
            if (ln != null && rn != null) {
                switch (op) {
                    case "==":
                        return ln.doubleValue() == rn.doubleValue();
                    case "!=":
                        return ln.doubleValue() != rn.doubleValue();
                    case ">":
                        return ln > rn;
                    case ">=":
                        return ln >= rn;
                    case "<":
                        return ln < rn;
                    case "<=":
                        return ln <= rn;
                    default:
                        return false;
                }
            }

            String ls = stringValue(l);
            String rs = stringValue(r);
            if (ls == null || rs == null) {
                return false;
            }
            switch (op) {
                case "==":
                    return ls.equalsIgnoreCase(rs);
                case "!=":
                    return !ls.equalsIgnoreCase(rs);
                default:
                    return false;
            }
        }
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        if (value instanceof String) {
            return !((String) value).isBlank();
        }
        return false;
    }

    private static Double toNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private enum TokenType {
        IDENT, NUMBER, STRING,
        AND, OR, NOT,
        EQ, NEQ, GT, GTE, LT, LTE,
        STARTS,
        LPAREN, RPAREN,
        EOF
    }

    private static class Token {
        private final TokenType type;
        private final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static class Lexer {
        private final String input;
        private int pos = 0;

        Lexer(String input) {
            this.input = input == null ? "" : input;
        }

        Token next() {
            skipWhitespace();
            if (pos >= input.length()) {
                return new Token(TokenType.EOF, "");
            }
            char c = input.charAt(pos);
            if (Character.isLetter(c) || c == '_') {
                String ident = readIdent();
                if ("startsWith".equalsIgnoreCase(ident)) {
                    return new Token(TokenType.STARTS, ident);
                }
                return new Token(TokenType.IDENT, ident);
            }
            if (Character.isDigit(c) || c == '.') {
                return new Token(TokenType.NUMBER, readNumber());
            }
            if (c == '"' || c == '\'') {
                return new Token(TokenType.STRING, readString());
            }
            if (match("&&")) return new Token(TokenType.AND, "&&");
            if (match("||")) return new Token(TokenType.OR, "||");
            if (match("==")) return new Token(TokenType.EQ, "==");
            if (match("!=")) return new Token(TokenType.NEQ, "!=");
            if (match(">=")) return new Token(TokenType.GTE, ">=");
            if (match("<=")) return new Token(TokenType.LTE, "<=");
            if (match(">")) return new Token(TokenType.GT, ">");
            if (match("<")) return new Token(TokenType.LT, "<");
            if (match("!")) return new Token(TokenType.NOT, "!");
            if (match("(")) return new Token(TokenType.LPAREN, "(");
            if (match(")")) return new Token(TokenType.RPAREN, ")");
            throw new IllegalArgumentException("Unexpected token at " + pos);
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private boolean match(String s) {
            if (input.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            return false;
        }

        private String readIdent() {
            int start = pos;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            return input.substring(start, pos);
        }

        private String readNumber() {
            int start = pos;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (Character.isDigit(c) || c == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            return input.substring(start, pos);
        }

        private String readString() {
            char quote = input.charAt(pos++);
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == quote) {
                    break;
                }
                if (c == '\\' && pos < input.length()) {
                    char next = input.charAt(pos++);
                    sb.append(next);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    private static class Parser {
        private final Lexer lexer;
        private Token current;

        Parser(String input) {
            this.lexer = new Lexer(input);
            this.current = lexer.next();
        }

        Expression parse() {
            Expression expr = parseOr();
            if (current.type != TokenType.EOF) {
                throw new IllegalArgumentException("Unexpected token: " + current.text);
            }
            return expr;
        }

        private Expression parseOr() {
            Expression expr = parseAnd();
            while (current.type == TokenType.OR) {
                String op = current.text;
                advance();
                Expression right = parseAnd();
                expr = new BinaryExpression(expr, op, right);
            }
            return expr;
        }

        private Expression parseAnd() {
            Expression expr = parseUnary();
            while (current.type == TokenType.AND) {
                String op = current.text;
                advance();
                Expression right = parseUnary();
                expr = new BinaryExpression(expr, op, right);
            }
            return expr;
        }

        private Expression parseUnary() {
            if (current.type == TokenType.NOT) {
                String op = current.text;
                advance();
                return new UnaryExpression(op, parseUnary());
            }
            return parseComparison();
        }

        private Expression parseComparison() {
            Expression expr = parsePrimary();
            if (isComparison(current.type)) {
                String op = current.text;
                advance();
                Expression right = parsePrimary();
                return new BinaryExpression(expr, op, right);
            }
            return expr;
        }

        private Expression parsePrimary() {
            switch (current.type) {
                case NUMBER:
                    double num = Double.parseDouble(current.text);
                    advance();
                    return new LiteralExpression(num);
                case STRING:
                    String str = current.text;
                    advance();
                    return new LiteralExpression(str);
                case IDENT:
                    String ident = current.text;
                    advance();
                    return new IdentifierExpression(ident);
                case LPAREN:
                    advance();
                    Expression expr = parseOr();
                    if (current.type != TokenType.RPAREN) {
                        throw new IllegalArgumentException("Missing )");
                    }
                    advance();
                    return expr;
                default:
                    throw new IllegalArgumentException("Unexpected token: " + current.text);
            }
        }

        private boolean isComparison(TokenType type) {
            return type == TokenType.EQ || type == TokenType.NEQ || type == TokenType.GT || type == TokenType.GTE
                    || type == TokenType.LT || type == TokenType.LTE || type == TokenType.STARTS;
        }

        private void advance() {
            current = lexer.next();
        }
    }
}
