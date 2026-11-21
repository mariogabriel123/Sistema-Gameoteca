package br.com.gameoteca.banco;

import br.com.gameoteca.erros.RegraNegocioException;
import br.com.gameoteca.modelo.Emprestimo;
import br.com.gameoteca.modelo.Jogo;
import br.com.gameoteca.modelo.Usuario;
import br.com.gameoteca.apoio.BancoDados;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmprestimoRepository {
    private final BancoDados banco;
    private final UsuarioRepository usuarios;
    private final JogoRepository jogos;

    public EmprestimoRepository(BancoDados banco, UsuarioRepository usuarios, JogoRepository jogos) {
        this.banco = banco;
        this.usuarios = usuarios;
        this.jogos = jogos;
    }

    public List<Emprestimo> listar(Long usuarioId) {
        return banco.consultar(conexao -> {
            String sql = "SELECT e.*,u.nome,j.titulo FROM emprestimos e "
                    + "JOIN usuarios u ON u.id=e.usuario_id JOIN jogos j ON j.id=e.jogo_id";
            if (usuarioId != null) sql += " WHERE e.usuario_id=?";
            sql += " ORDER BY e.devolucao IS NOT NULL,e.prevista,e.id DESC";
            List<Emprestimo> lista = new ArrayList<>();
            try (var comando = conexao.prepareStatement(sql)) {
                if (usuarioId != null) comando.setLong(1, usuarioId);
                try (var resultado = comando.executeQuery()) {
                    while (resultado.next()) {
                        String devolucao = resultado.getString("devolucao");
                        lista.add(new Emprestimo(resultado.getLong("id"), resultado.getLong("usuario_id"),
                                resultado.getString("nome"), resultado.getString("titulo"),
                                LocalDate.parse(resultado.getString("retirada")),
                                LocalDate.parse(resultado.getString("prevista")),
                                devolucao == null ? null : LocalDate.parse(devolucao)));
                    }
                }
            }
            return lista;
        });
    }

    /** A transação reúne a validação de disponibilidade e o registro da retirada. */
    public void emprestar(long solicitanteId, long usuarioId, long jogoId) {
        banco.transacao(conexao -> {
            Usuario solicitante = usuarios.buscar(conexao, solicitanteId);
            if (solicitante == null || !solicitante.isAtivo())
                throw new RegraNegocioException("Entre novamente com uma conta ativa.");
            if (!solicitante.isAdministrador() && solicitanteId != usuarioId)
                throw new RegraNegocioException("Você só pode solicitar jogos para sua própria conta.");
            Usuario usuario = usuarios.buscar(conexao, usuarioId);
            if (usuario == null || !usuario.isAtivo()) throw new RegraNegocioException("Selecione um usuário ativo.");
            Jogo jogo = jogos.buscar(conexao, jogoId);
            if (jogo == null || !jogo.isAtivo()) throw new RegraNegocioException("Selecione um jogo ativo.");
            if (jogo.getDisponiveis() <= 0) throw new RegraNegocioException("Todas as unidades deste jogo estão em uso.");

            LocalDate hoje = LocalDate.now();
            String sql = "SELECT jogo_id,prevista FROM emprestimos WHERE usuario_id=? AND devolucao IS NULL";
            try (var comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, usuarioId);
                int quantidade = 0;
                try (var resultado = comando.executeQuery()) {
                    while (resultado.next()) {
                        quantidade++;
                        if (resultado.getLong("jogo_id") == jogoId)
                            throw new RegraNegocioException("Este usuário já está com uma unidade deste jogo.");
                        if (LocalDate.parse(resultado.getString("prevista")).isBefore(hoje))
                            throw new RegraNegocioException("Regularize as devoluções atrasadas antes de solicitar outro jogo.");
                    }
                }
                if (quantidade >= 3) throw new RegraNegocioException("O limite é de 3 empréstimos em andamento por usuário.");
            }

            // POLIMORFISMO: o objeto concreto decide o prazo de seu tipo de jogo.
            LocalDate prevista = hoje.plusDays(jogo.getPrazoDias());
            try (var comando = conexao.prepareStatement("INSERT INTO emprestimos(usuario_id,jogo_id,retirada,prevista) VALUES(?,?,?,?)")) {
                comando.setLong(1, usuarioId);
                comando.setLong(2, jogoId);
                comando.setString(3, hoje.toString());
                comando.setString(4, prevista.toString());
                comando.executeUpdate();
            }
            return null;
        });
    }

    /** ID nulo limpa o histórico; ID informado exclui somente aquele registro devolvido. */
    public int excluirHistorico(long solicitanteId, Long emprestimoId) {
        return banco.transacao(conexao -> {
            Usuario solicitante = usuarios.buscar(conexao, solicitanteId);
            if (solicitante == null || !solicitante.isAtivo() || !solicitante.isAdministrador())
                throw new RegraNegocioException("Somente um administrador pode excluir o histórico.");

            String sql = "DELETE FROM emprestimos WHERE devolucao IS NOT NULL";
            if (emprestimoId != null) sql += " AND id=?";
            try (var comando = conexao.prepareStatement(sql)) {
                if (emprestimoId != null) comando.setLong(1, emprestimoId);
                int excluidos = comando.executeUpdate();
                if (emprestimoId != null && excluidos == 0)
                    throw new RegraNegocioException("Registro não encontrado ou empréstimo ainda em andamento.");
                return excluidos;
            }
        });
    }

    public void devolver(long solicitanteId, long emprestimoId) {
        banco.transacao(conexao -> {
            Usuario solicitante = usuarios.buscar(conexao, solicitanteId);
            if (solicitante == null || !solicitante.isAtivo() || !solicitante.isAdministrador())
                throw new RegraNegocioException("Um administrador deve confirmar a devolução.");
            try (var comando = conexao.prepareStatement("UPDATE emprestimos SET devolucao=? WHERE id=? AND devolucao IS NULL")) {
                comando.setString(1, LocalDate.now().toString());
                comando.setLong(2, emprestimoId);
                if (comando.executeUpdate() == 0)
                    throw new RegraNegocioException("Empréstimo não encontrado ou já devolvido.");
            }
            return null;
        });
    }
}
