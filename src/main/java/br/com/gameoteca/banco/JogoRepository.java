package br.com.gameoteca.banco;

import br.com.gameoteca.erros.RegraNegocioException;
import br.com.gameoteca.modelo.Jogo;
import br.com.gameoteca.modelo.JogoFisico;
import br.com.gameoteca.modelo.JogoLocal;
import br.com.gameoteca.apoio.BancoDados;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JogoRepository implements CrudRepository<Jogo> {
    private final BancoDados banco;
    private static final String CONSULTA = "SELECT j.*, (SELECT COUNT(*) FROM emprestimos e "
            + "WHERE e.jogo_id=j.id AND e.devolucao IS NULL) em_uso FROM jogos j";

    public JogoRepository(BancoDados banco) { this.banco = banco; }

    private Jogo ler(ResultSet resultado) throws SQLException {
        Long id = resultado.getLong("id");
        String titulo = resultado.getString("titulo");
        String genero = resultado.getString("genero");
        String plataforma = resultado.getString("plataforma");
        int quantidade = resultado.getInt("quantidade");
        int emUso = resultado.getInt("em_uso");
        boolean ativo = resultado.getBoolean("ativo");
        if (resultado.getString("modalidade").equals("FISICO"))
            return new JogoFisico(id, titulo, genero, plataforma, quantidade, emUso, ativo);
        return new JogoLocal(id, titulo, genero, plataforma, quantidade, emUso, ativo);
    }

    @Override
    public List<Jogo> listar() {
        return banco.consultar(conexao -> {
            List<Jogo> lista = new ArrayList<>();
            try (var comando = conexao.prepareStatement(CONSULTA + " ORDER BY j.titulo COLLATE NOCASE");
                 var resultado = comando.executeQuery()) {
                while (resultado.next()) lista.add(ler(resultado));
            }
            return lista;
        });
    }

    public Jogo buscar(Connection conexao, long id) throws SQLException {
        try (var comando = conexao.prepareStatement(CONSULTA + " WHERE j.id=?")) {
            comando.setLong(1, id);
            try (var resultado = comando.executeQuery()) {
                return resultado.next() ? ler(resultado) : null;
            }
        }
    }

    @Override
    public Jogo buscarPorId(long id) { return banco.consultar(conexao -> buscar(conexao, id)); }

    @Override
    public Jogo salvar(Jogo jogo) {
        return banco.transacao(conexao -> {
            if (jogo.getId() != null) {
                Jogo anterior = buscar(conexao, jogo.getId());
                if (anterior == null) throw new RegraNegocioException("Jogo não encontrado.");
                if (jogo.getQuantidade() < anterior.getEmUso())
                    throw new RegraNegocioException("A quantidade não pode ficar abaixo das unidades em uso.");
                if (anterior.getEmUso() > 0 && (!jogo.getModalidade().equals(anterior.getModalidade())
                        || !jogo.getPlataforma().equals(anterior.getPlataforma())))
                    throw new RegraNegocioException("Finalize os empréstimos antes de mudar plataforma ou modalidade.");
            }
            String sql = jogo.getId() == null
                    ? "INSERT INTO jogos(titulo,genero,plataforma,modalidade,quantidade,ativo) VALUES(?,?,?,?,?,?)"
                    : "UPDATE jogos SET titulo=?,genero=?,plataforma=?,modalidade=?,quantidade=?,ativo=? WHERE id=?";
            try (var comando = conexao.prepareStatement(sql)) {
                comando.setString(1, jogo.getTitulo());
                comando.setString(2, jogo.getGenero());
                comando.setString(3, jogo.getPlataforma());
                comando.setString(4, jogo.getModalidade());
                comando.setInt(5, jogo.getQuantidade());
                comando.setBoolean(6, jogo.isAtivo());
                if (jogo.getId() != null) comando.setLong(7, jogo.getId());
                comando.executeUpdate();
            }
            long id;
            if (jogo.getId() == null) {
                try (var comando = conexao.createStatement(); var resultado = comando.executeQuery("SELECT last_insert_rowid()")) {
                    resultado.next();
                    id = resultado.getLong(1);
                }
            } else id = jogo.getId();
            return buscar(conexao, id);
        });
    }

    @Override
    public void excluir(long id) {
        banco.transacao(conexao -> {
            try (var comando = conexao.prepareStatement("DELETE FROM jogos WHERE id=?")) {
                comando.setLong(1, id);
                if (comando.executeUpdate() == 0) throw new RegraNegocioException("Jogo não encontrado.");
            }
            return null;
        });
    }
}
