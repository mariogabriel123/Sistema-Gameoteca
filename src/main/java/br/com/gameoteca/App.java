package br.com.gameoteca;

import br.com.gameoteca.controle.GameotecaController;
import br.com.gameoteca.banco.EmprestimoRepository;
import br.com.gameoteca.banco.JogoRepository;
import br.com.gameoteca.banco.UsuarioRepository;
import br.com.gameoteca.regras.GameotecaService;
import br.com.gameoteca.apoio.BancoDados;
import br.com.gameoteca.telas.LoginFrame;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Ponto de entrada: prepara o banco e abre a tela de login. */
public class App {
    public static void main(String[] args) {
        try {
            String caminho = System.getProperty("gameoteca.db", BancoDados.caminhoPadrao(App.class).toString());
            BancoDados banco = new BancoDados(Path.of(caminho));
            banco.inicializar();
            UsuarioRepository usuarios = new UsuarioRepository(banco);
            JogoRepository jogos = new JogoRepository(banco);
            EmprestimoRepository emprestimos = new EmprestimoRepository(banco, usuarios, jogos);
            GameotecaService service = new GameotecaService(usuarios, jogos, emprestimos);
            GameotecaController controller = new GameotecaController(service);
            SwingUtilities.invokeLater(() -> new LoginFrame(controller).setVisible(true));
        } catch (RuntimeException erro) {
            erro.printStackTrace();
            JOptionPane.showMessageDialog(null, "Não foi possível iniciar a Gameoteca. " + erro.getMessage());
        }
    }
}
