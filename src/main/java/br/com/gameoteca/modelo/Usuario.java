package br.com.gameoteca.modelo;

public class Usuario {
    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final boolean administrador;
    private final boolean ativo;

    public Usuario(Long id, String nome, String email, String senhaHash,
                   boolean administrador, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.administrador = administrador;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public boolean isAdministrador() { return administrador; }
    public boolean isAtivo() { return ativo; }

    @Override
    public String toString() {
        return nome + " (" + email + ")";
    }
}
