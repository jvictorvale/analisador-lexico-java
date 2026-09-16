package br.com.vale;

// Classe para armazenar os dados de cada Token
class Token {
    TokenType type;
    String lexeme;
    String attribute;

    public Token(TokenType type, String lexeme, String attribute) {
        this.type = type;
        this.lexeme = lexeme;
        this.attribute = attribute;
    }

    @Override
    public String toString() {
        if (attribute != null && !attribute.isEmpty()) {
            return String.format("<%s, '%s', %s>", type, lexeme, attribute);
        }
        return String.format("<%s, '%s'>", type, lexeme);
    }
}
