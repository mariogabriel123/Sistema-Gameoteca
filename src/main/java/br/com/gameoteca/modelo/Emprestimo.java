package br.com.gameoteca.modelo;

import java.time.LocalDate;

public class Emprestimo {
    private final long id;
    private final long usuarioId;
    private final String usuarioNome;
    private final String jogoTitulo;
    private final LocalDate retirada;
    private final LocalDate prevista;
    private final LocalDate devolucao;

    public Emprestimo(long id, long usuarioId, String usuarioNome, String jogoTitulo,
                      LocalDate retirada, LocalDate prevista, LocalDate devolucao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.jogoTitulo = jogoTitulo;
        this.retirada = retirada;
        this.prevista = prevista;
        this.devolucao = devolucao;
    }

    public long getId() { return id; }
    public long getUsuarioId() { return usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public String getJogoTitulo() { return jogoTitulo; }
    public LocalDate getRetirada() { return retirada; }
    public LocalDate getPrevista() { return prevista; }
    public LocalDate getDevolucao() { return devolucao; }

    public boolean isAberto() {
        return devolucao == null;
    }

    public String getSituacao() {
        if (!isAberto()) return "Devolvido";
        if (prevista.isBefore(LocalDate.now())) return "Atrasado";
        return "Em andamento";
    }
}
