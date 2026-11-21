package br.com.gameoteca.modelo;

/** Classe abstrata: dados comuns aos dois tipos de jogo. */
public abstract class Jogo {
    private final Long id;
    private final String titulo;
    private final String genero;
    private final String plataforma;
    private final int quantidade;
    private final int emUso;
    private final boolean ativo;

    public Jogo(Long id, String titulo, String genero, String plataforma,
                int quantidade, int emUso, boolean ativo) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
        this.plataforma = plataforma;
        this.quantidade = quantidade;
        this.emUso = emUso;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getGenero() { return genero; }
    public String getPlataforma() { return plataforma; }
    public int getQuantidade() { return quantidade; }
    public int getEmUso() { return emUso; }
    public boolean isAtivo() { return ativo; }

    public int getDisponiveis() {
        return quantidade - emUso;
    }

    public abstract String getModalidade();
    public abstract int getPrazoDias();

    @Override
    public String toString() {
        return titulo + " - " + plataforma + " (" + getDisponiveis() + " disponíveis)";
    }
}
