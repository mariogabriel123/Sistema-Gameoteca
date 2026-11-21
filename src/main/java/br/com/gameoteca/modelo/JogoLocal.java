package br.com.gameoteca.modelo;

/** Acesso nas estações da ludoteca, encerrado no mesmo dia. */
public class JogoLocal extends Jogo {
    public JogoLocal(Long id, String titulo, String genero, String plataforma,
                     int quantidade, int emUso, boolean ativo) {
        super(id, titulo, genero, plataforma, quantidade, emUso, ativo);
    }

    @Override
    public String getModalidade() {
        return "LOCAL";
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
