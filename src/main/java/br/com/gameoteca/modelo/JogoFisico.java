package br.com.gameoteca.modelo;

/** Herança: aproveita os dados de Jogo e define o prazo da mídia física. */
public class JogoFisico extends Jogo {
    public JogoFisico(Long id, String titulo, String genero, String plataforma,
                      int quantidade, int emUso, boolean ativo) {
        super(id, titulo, genero, plataforma, quantidade, emUso, ativo);
    }

    @Override
    public String getModalidade() {
        return "FISICO";
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }
}
