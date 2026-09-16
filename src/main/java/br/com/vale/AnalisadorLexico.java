package br.com.vale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalisadorLexico {
    private char[] buffer;
    private int beginPointer = 0;
    private int forwardPointer = 0;

    // Tabela de Símbolos apenas para identificadores
    private Map<String, Integer> tabelaDeSimbolos = new HashMap<>();

    // Lista de palavras-chave reservadas
    private static final List<String> KEYWORDS = List.of(
            "public", "static", "void", "int", "float", "char", "if", "else",
            "while", "return", "class", "boolean", "true", "false", "for",
            "do", "break", "continue", "switch", "case"
    );

    public AnalisadorLexico(String filePath) throws IOException {
        String sourceCode = new String(Files.readAllBytes(Paths.get(filePath)));
        this.buffer = (sourceCode + '\0').toCharArray();
    }

    public List<Token> analisar() {
        List<Token> tokens = new ArrayList<>();

        while (forwardPointer < buffer.length - 1) {
            beginPointer = forwardPointer;
            char currentChar = buffer[forwardPointer];

            // Ignorar espaços em branco, quebras de linha e tabulações
            if (Character.isWhitespace(currentChar)) {
                forwardPointer++;
                continue;
            }

            // Comentários (Linha e Bloco)
            if (currentChar == '/' && forwardPointer + 1 < buffer.length) {
                if (buffer[forwardPointer + 1] == '/') {
                    while (buffer[forwardPointer] != '\n' && buffer[forwardPointer] != '\0') {
                        forwardPointer++;
                    }
                    continue;
                } else if (buffer[forwardPointer + 1] == '*') {
                    forwardPointer += 2;
                    while (forwardPointer < buffer.length - 1 && !(buffer[forwardPointer] == '*' && buffer[forwardPointer + 1] == '/')) {
                        forwardPointer++;
                    }
                    if (buffer[forwardPointer] != '\0') forwardPointer += 2;
                    continue;
                }
            }

            // Identificadores e Palavras-chave
            if (Character.isLetter(currentChar) || currentChar == '_') {
                while (Character.isLetterOrDigit(buffer[forwardPointer]) || buffer[forwardPointer] == '_') {
                    forwardPointer++;
                }
                String lexeme = new String(buffer, beginPointer, forwardPointer - beginPointer);

                if (KEYWORDS.contains(lexeme)) {
                    tokens.add(new Token(TokenType.KEYWORD, lexeme, null));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, lexeme, null));
                    tabelaDeSimbolos.put(lexeme, tabelaDeSimbolos.getOrDefault(lexeme, 0) + 1);
                }
                continue;
            }

            // Números e Validação de Erros (Vírgula e Variável iniciando com número)
            if (Character.isDigit(currentChar)) {
                boolean hasDot = false;
                boolean isError = false;
                String errorReason = "";

                while (Character.isDigit(buffer[forwardPointer]) || buffer[forwardPointer] == '.' || buffer[forwardPointer] == ',') {
                    if (buffer[forwardPointer] == '.') {
                        if (hasDot) isError = true;
                        hasDot = true;
                    } else if (buffer[forwardPointer] == ',') {
                        isError = true;
                        errorReason = "Uso incorreto de virgula como separador decimal";
                    }
                    forwardPointer++;
                }

                if (Character.isLetter(buffer[forwardPointer]) || buffer[forwardPointer] == '_') {
                    isError = true;
                    errorReason = "Nome de variaveis nao podem comecar com numeros";
                    while (Character.isLetterOrDigit(buffer[forwardPointer]) || buffer[forwardPointer] == '_') {
                        forwardPointer++;
                    }
                }

                String lexeme = new String(buffer, beginPointer, forwardPointer - beginPointer);
                if (isError) {
                    tokens.add(new Token(TokenType.ERROR, lexeme, errorReason));
                } else if (hasDot) {
                    tokens.add(new Token(TokenType.FLOAT_LITERAL, lexeme, null));
                } else {
                    tokens.add(new Token(TokenType.INT_LITERAL, lexeme, null));
                }
                continue;
            }

            // Strings e Erro de "String não terminada"
            if (currentChar == '"') {
                forwardPointer++;
                while (buffer[forwardPointer] != '"' && buffer[forwardPointer] != '\n' && buffer[forwardPointer] != '\0') {
                    forwardPointer++;
                }

                if (buffer[forwardPointer] == '"') {
                    forwardPointer++;
                    String lexeme = new String(buffer, beginPointer, forwardPointer - beginPointer);
                    tokens.add(new Token(TokenType.STRING_LITERAL, lexeme, null));
                } else {
                    String lexeme = new String(buffer, beginPointer, forwardPointer - beginPointer);
                    tokens.add(new Token(TokenType.ERROR, lexeme, "String nao terminada"));
                }
                continue;
            }

            // Caracteres
            if (currentChar == '\'') {
                forwardPointer++;
                if (buffer[forwardPointer] == '\\') forwardPointer += 2;
                else forwardPointer++;

                if (buffer[forwardPointer] == '\'') {
                    forwardPointer++;
                    String lexeme = new String(buffer, beginPointer, forwardPointer - beginPointer);
                    tokens.add(new Token(TokenType.CHAR_LITERAL, lexeme, null));
                }
                continue;
            }

            // Operadores Compostos
            if (forwardPointer + 1 < buffer.length) {
                char nextChar = buffer[forwardPointer + 1];
                String lookaheadStr = "" + currentChar + nextChar;
                String[] opCompostos = {"==", "!=", "<=", ">=", "&&", "||", "+=", "-=", "/="};

                boolean isComposto = false;
                for (String op : opCompostos) {
                    if (lookaheadStr.equals(op)) {
                        tokens.add(new Token(TokenType.OPERATOR, lookaheadStr, null));
                        forwardPointer += 2;
                        isComposto = true;
                        break;
                    }
                }
                if (isComposto) continue;
            }

            // Operadores Simples e Delimitadores
            String operadoresSimples = "+-*/%=!<>";
            String delimitadores = ";, .(){}[]";

            if (operadoresSimples.indexOf(currentChar) != -1) {
                tokens.add(new Token(TokenType.OPERATOR, String.valueOf(currentChar), null));
                forwardPointer++;
                continue;
            } else if (delimitadores.indexOf(currentChar) != -1) {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(currentChar), null));
                forwardPointer++;
                continue;
            }

            // Símbolo inválido
            tokens.add(new Token(TokenType.ERROR, String.valueOf(currentChar), "Simbolo invalido"));
            forwardPointer++;
        }

        return tokens;
    }

    // Método para formatar a saída conforme exigido no trabalho
    public void imprimirResultados(List<Token> tokens) {
        System.out.println("====== TABELA DE TOKENS ======");
        System.out.printf("%-15s | %-20s | %s\n", "TIPO", "LEXEMA", "ATRIBUTO/ERRO");
        System.out.println("-".repeat(70));
        for (Token t : tokens) {
            String atributoFormatado = (t.attribute != null) ? t.attribute : "-";

            System.out.printf("%-15s | %-20s | %s\n", t.type, t.lexeme.replace("\n", "\\n").replace("\r", ""), atributoFormatado);
        }

        System.out.println("\n====== TABELA DE SIMBOLOS (Identificadores) ======");
        System.out.printf("%-20s | %s\n", "IDENTIFICADOR", "OCORRENCIAS");
        System.out.println("-".repeat(40));
        for (Map.Entry<String, Integer> entry : tabelaDeSimbolos.entrySet()) {
            System.out.printf("%-20s | %d\n", entry.getKey(), entry.getValue());
        }
        System.out.println("\n");
    }

    public static void main(String[] args) {
        try {
            System.out.println(">>> 1. INICIANDO ANALISE DE SUCESSO <<<");
            AnalisadorLexico lexerSucesso = new AnalisadorLexico("ExemploSucesso.java");
            List<Token> tokensSucesso = lexerSucesso.analisar();
            lexerSucesso.imprimirResultados(tokensSucesso);

            System.out.println(">>> 2. INICIANDO ANALISE COM ERROS <<<");
            AnalisadorLexico lexerErro = new AnalisadorLexico("ExemploErro.java");
            List<Token> tokensErro = lexerErro.analisar();
            lexerErro.imprimirResultados(tokensErro);

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: Certifique-se de que os arquivos 'ExemploSucesso.java' e 'ExemploErro.java' estao na mesma pasta do projeto.");
        }
    }
}