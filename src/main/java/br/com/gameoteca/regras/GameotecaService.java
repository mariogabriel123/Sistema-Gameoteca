package br.com.gameoteca.regras;

import br.com.gameoteca.erros.RegraNegocioException;
import br.com.gameoteca.modelo.*;
import br.com.gameoteca.banco.*;
import br.com.gameoteca.apoio.Senhas;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Reúne as regras dos quatro casos de uso do sistema pequeno. */
public class GameotecaService {
    private final UsuarioRepository usuarios;
    private final CrudRepository<Jogo> jogos;
    private final EmprestimoRepository emprestimos;
    private Usuario logado;

    public GameotecaService(UsuarioRepository usuarios, CrudRepository<Jogo> jogos,
                            EmprestimoRepository emprestimos) {
        this.usuarios = usuarios;
        this.jogos = jogos;
        this.emprestimos = emprestimos;
    }

    public boolean primeiroAcesso() { return usuarios.estaVazio(); }

    public void criarAdministrador(String nome, String email, char[] senha, char[] confirmacao) {
        try {
            String nomeValido = texto(nome, "o nome", 100);
            String emailValido = email(email);
            validarSenha(senha);
            if (!Arrays.equals(senha, confirmacao)) throw new RegraNegocioException("As senhas não conferem.");
            Usuario usuario = new Usuario(null, nomeValido, emailValido, Senhas.gerar(senha), true, true);
            logado = usuarios.criarPrimeiroAdministrador(usuario);
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    public void entrar(String email, char[] senha) {
        logado = null;
        try {
            Usuario usuario = usuarios.buscarPorEmail(email(email));
            if (senha.length > 128 || usuario == null || !usuario.isAtivo()
                    || !Senhas.conferir(senha, usuario.getSenhaHash()))
                throw new RegraNegocioException("E-mail ou senha inválidos, ou conta desativada.");
            logado = usuario;
        } finally { Arrays.fill(senha, '\0'); }
    }

    /** Cadastro pela tela de login: toda nova conta começa como membro. */
    public void criarConta(String nome, String email, char[] senha, char[] confirmacao) {
        try {
            if (usuarios.estaVazio()) throw new RegraNegocioException("Crie primeiro a conta de administrador.");
            String nomeValido = texto(nome, "o nome", 100);
            String emailValido = email(email);
            validarSenha(senha);
            if (!Arrays.equals(senha, confirmacao)) throw new RegraNegocioException("As senhas não conferem.");
            Usuario usuario = new Usuario(null, nomeValido, emailValido, Senhas.gerar(senha), false, true);
            logado = usuarios.salvar(usuario);
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    public void sair() { logado = null; }

    public Usuario usuarioAtual() {
        if (logado == null) throw new RegraNegocioException("Entre com sua conta.");
        Usuario usuario = usuarios.buscarPorId(logado.getId());
        if (usuario == null || !usuario.isAtivo()) throw new RegraNegocioException("Sua conta está indisponível. Entre novamente.");
        return usuario;
    }

    private Usuario exigirAdministrador() {
        Usuario usuario = usuarioAtual();
        if (!usuario.isAdministrador()) throw new RegraNegocioException("Esta ação é exclusiva do administrador.");
        return usuario;
    }

    public List<Usuario> listarUsuarios() {
        exigirAdministrador();
        return usuarios.listar();
    }

    public void salvarUsuario(Long id, String nome, String email, boolean administrador, boolean ativo, char[] senha) {
        try {
            Usuario atual = exigirAdministrador();
            if (id != null && id.equals(atual.getId()) && (!ativo || !administrador))
                throw new RegraNegocioException("Você não pode desativar sua própria conta nem retirar seu perfil de administrador.");
            String nomeValido = texto(nome, "o nome", 100);
            String emailValido = email(email);
            String hash;
            if (id == null || senha.length > 0) {
                validarSenha(senha);
                hash = Senhas.gerar(senha);
            } else {
                Usuario anterior = usuarios.buscarPorId(id);
                if (anterior == null) throw new RegraNegocioException("Usuário não encontrado.");
                hash = anterior.getSenhaHash();
            }
            usuarios.salvar(new Usuario(id, nomeValido, emailValido, hash, administrador, ativo));
        } finally { Arrays.fill(senha, '\0'); }
    }

    public void excluirUsuario(long id) {
        Usuario atual = exigirAdministrador();
        if (atual.getId() == id) throw new RegraNegocioException("Você não pode excluir a conta que está usando.");
        usuarios.excluir(id);
    }

    public List<Jogo> listarJogos() {
        Usuario atual = usuarioAtual();
        List<Jogo> lista = new ArrayList<>();
        for (Jogo jogo : jogos.listar()) {
            if (atual.isAdministrador() || jogo.isAtivo()) lista.add(jogo);
        }
        return lista;
    }

    public void salvarJogo(Long id, String titulo, String genero, String plataforma,
                           boolean fisico, int quantidade, boolean ativo) {
        exigirAdministrador();
        titulo = texto(titulo, "o título", 120);
        genero = texto(genero, "o gênero", 60);
        List<String> plataformas = Arrays.asList("PC", "PS4", "PS5", "XBOX_ONE", "XBOX_SERIES", "SWITCH");
        if (!plataformas.contains(plataforma)) throw new RegraNegocioException("Selecione uma plataforma válida.");
        if (quantidade < 1 || quantidade > 999) throw new RegraNegocioException("A quantidade deve ser de 1 a 999.");
        Jogo jogo;
        if (fisico) jogo = new JogoFisico(id, titulo, genero, plataforma, quantidade, 0, ativo);
        else jogo = new JogoLocal(id, titulo, genero, plataforma, quantidade, 0, ativo);
        jogos.salvar(jogo);
    }

    public void excluirJogo(long id) {
        exigirAdministrador();
        jogos.excluir(id);
    }

    public List<Usuario> usuariosParaEmprestimo() {
        Usuario atual = usuarioAtual();
        List<Usuario> lista = new ArrayList<>();
        if (atual.isAdministrador()) {
            for (Usuario usuario : usuarios.listar()) if (usuario.isAtivo()) lista.add(usuario);
        } else lista.add(atual);
        return lista;
    }

    public List<Emprestimo> listarEmprestimos() {
        Usuario atual = usuarioAtual();
        Long filtro = atual.isAdministrador() ? null : atual.getId();
        return emprestimos.listar(filtro);
    }

    public void emprestar(long usuarioId, long jogoId) {
        emprestimos.emprestar(usuarioAtual().getId(), usuarioId, jogoId);
    }

    public void devolver(long id) {
        emprestimos.devolver(exigirAdministrador().getId(), id);
    }

    public int excluirHistorico(Long id) {
        return emprestimos.excluirHistorico(exigirAdministrador().getId(), id);
    }

    private String texto(String valor, String campo, int maximo) {
        String limpo = valor == null ? "" : valor.trim();
        if (limpo.isEmpty()) throw new RegraNegocioException("Informe " + campo + ".");
        if (limpo.length() > maximo) throw new RegraNegocioException("Use até " + maximo + " caracteres para " + campo + ".");
        return limpo;
    }

    private String email(String valor) {
        String limpo = texto(valor, "o e-mail", 150).toLowerCase(Locale.ROOT);
        if (!limpo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new RegraNegocioException("Informe um e-mail válido.");
        return limpo;
    }

    private void validarSenha(char[] senha) {
        if (senha.length < 8 || senha.length > 128) throw new RegraNegocioException("A senha deve ter de 8 a 128 caracteres.");
    }
}
