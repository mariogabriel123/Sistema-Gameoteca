package br.com.gameoteca.banco;

import br.com.gameoteca.erros.RegraNegocioException;
import br.com.gameoteca.modelo.Usuario;
import br.com.gameoteca.apoio.BancoDados;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository implements CrudRepository<Usuario> {
    private final BancoDados banco;

    public UsuarioRepository(BancoDados banco) { this.banco = banco; }

    private Usuario ler(ResultSet resultado) throws SQLException {
        return new Usuario(resultado.getLong("id"), resultado.getString("nome"),
                resultado.getString("email"), resultado.getString("senha_hash"),
                resultado.getString("perfil").equals("ADMIN"), resultado.getBoolean("ativo"));
    }

    @Override
    public List<Usuario> listar() {
        return banco.consultar(conexao -> {
            List<Usuario> lista = new ArrayList<>();
            try (var comando = conexao.prepareStatement("SELECT * FROM usuarios ORDER BY nome COLLATE NOCASE");
                 var resultado = comando.executeQuery()) {
                while (resultado.next()) lista.add(ler(resultado));
            }
            return lista;
        });
    }

    public Usuario buscar(Connection conexao, long id) throws SQLException {
        try (var comando = conexao.prepareStatement("SELECT * FROM usuarios WHERE id=?")) {
            comando.setLong(1, id);
            try (var resultado = comando.executeQuery()) {
                return resultado.next() ? ler(resultado) : null;
            }
        }
    }

    @Override
    public Usuario buscarPorId(long id) { return banco.consultar(conexao -> buscar(conexao, id)); }

    public Usuario buscarPorEmail(String email) {
        return banco.consultar(conexao -> {
            try (var comando = conexao.prepareStatement("SELECT * FROM usuarios WHERE email=?")) {
                comando.setString(1, email);
                try (var resultado = comando.executeQuery()) {
                    return resultado.next() ? ler(resultado) : null;
                }
            }
        });
    }

    public boolean estaVazio() {
        return banco.consultar(conexao -> {
            try (var comando = conexao.createStatement();
                 var resultado = comando.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                return resultado.next() && resultado.getInt(1) == 0;
            }
        });
    }

    @Override
    public Usuario salvar(Usuario usuario) { return gravar(usuario, false); }

    public Usuario criarPrimeiroAdministrador(Usuario usuario) { return gravar(usuario, true); }

    private Usuario gravar(Usuario usuario, boolean primeiroAcesso) {
        return banco.transacao(conexao -> {
            if (primeiroAcesso) {
                try (var comando = conexao.createStatement();
                     var resultado = comando.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                    if (resultado.next() && resultado.getInt(1) > 0)
                        throw new RegraNegocioException("O administrador inicial já foi criado. Entre com sua conta.");
                }
            }
            String sql;
            if (usuario.getId() == null) {
                sql = "INSERT INTO usuarios(nome,email,senha_hash,perfil,ativo) VALUES(?,?,?,?,?)";
            } else {
                Usuario anterior = buscar(conexao, usuario.getId());
                if (anterior == null) throw new RegraNegocioException("Usuário não encontrado.");
                protegerAdministrador(conexao, anterior, usuario.isAdministrador() && usuario.isAtivo());
                sql = "UPDATE usuarios SET nome=?,email=?,senha_hash=?,perfil=?,ativo=? WHERE id=?";
            }
            try (var comando = conexao.prepareStatement(sql)) {
                comando.setString(1, usuario.getNome());
                comando.setString(2, usuario.getEmail());
                comando.setString(3, usuario.getSenhaHash());
                comando.setString(4, usuario.isAdministrador() ? "ADMIN" : "MEMBRO");
                comando.setBoolean(5, usuario.isAtivo());
                if (usuario.getId() != null) comando.setLong(6, usuario.getId());
                comando.executeUpdate();
            }
            long id;
            if (usuario.getId() == null) {
                try (var comando = conexao.createStatement(); var resultado = comando.executeQuery("SELECT last_insert_rowid()")) {
                    resultado.next();
                    id = resultado.getLong(1);
                }
            } else id = usuario.getId();
            return buscar(conexao, id);
        });
    }

    private void protegerAdministrador(Connection conexao, Usuario anterior, boolean continuaAdmin) throws SQLException {
        if (anterior.isAdministrador() && anterior.isAtivo() && !continuaAdmin) {
            try (var comando = conexao.createStatement();
                 var resultado = comando.executeQuery("SELECT COUNT(*) FROM usuarios WHERE perfil='ADMIN' AND ativo=1")) {
                if (resultado.next() && resultado.getInt(1) <= 1)
                    throw new RegraNegocioException("Mantenha pelo menos um administrador ativo.");
            }
        }
    }

    @Override
    public void excluir(long id) {
        banco.transacao(conexao -> {
            Usuario usuario = buscar(conexao, id);
            if (usuario == null) throw new RegraNegocioException("Usuário não encontrado.");
            protegerAdministrador(conexao, usuario, false);
            try (var comando = conexao.prepareStatement("DELETE FROM usuarios WHERE id=?")) {
                comando.setLong(1, id);
                comando.executeUpdate();
            }
            return null;
        });
    }
}
