package br.com.gameoteca.telas;

import br.com.gameoteca.controle.GameotecaController;
import br.com.gameoteca.erros.RegraNegocioException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/** Tela de entrada; no primeiro uso também cadastra o administrador. */
public class LoginFrame extends JFrame {
    // Cores e fontes são apenas configurações visuais da janela.
    private final Color roxo = new Color(101, 66, 212);
    private final Font fonte = new Font("SansSerif", Font.PLAIN, 14);
    private final GameotecaController controller;
    private final boolean primeiroAcesso;
    private boolean cadastrando;
    private final JPanel campos = new JPanel(new GridLayout(0, 1, 0, 9));
    private final JTextField nome = new JTextField(24);
    private final JTextField email = new JTextField(24);
    private final JPasswordField senha = new JPasswordField(24);
    private final JPasswordField confirmacao = new JPasswordField(24);
    private final JButton entrar = new JButton();
    private final JButton alternar = new JButton();

    public LoginFrame(GameotecaController controller) {
        super("Gameoteca - Login");
        this.controller = controller;
        primeiroAcesso = controller.primeiroAcesso();
        cadastrando = primeiroAcesso;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout(0, 26));
        painel.setBackground(Color.WHITE);
        painel.setBorder(new EmptyBorder(34, 40, 34, 40));
        JPanel cabecalho = new JPanel(new BorderLayout(0, 9));
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Gameoteca", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 32));
        titulo.setForeground(roxo);
        JLabel descricao = new JLabel("Jogos de console e PC, em um só lugar.", SwingConstants.CENTER);
        descricao.setFont(fonte);
        descricao.setForeground(new Color(100, 105, 125));
        cabecalho.add(titulo, BorderLayout.NORTH);
        cabecalho.add(descricao, BorderLayout.SOUTH);
        painel.add(cabecalho, BorderLayout.NORTH);

        campos.setOpaque(false);
        painel.add(campos, BorderLayout.CENTER);
        entrar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        entrar.setBackground(roxo);
        entrar.setForeground(Color.WHITE);
        entrar.setFont(fonte.deriveFont(Font.BOLD));
        entrar.setBorder(new EmptyBorder(13, 18, 13, 18));
        entrar.setFocusPainted(false);
        entrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entrar.addActionListener(evento -> autenticar());
        JPanel botoes = new JPanel(new GridLayout(0, 1, 0, 10));
        botoes.setOpaque(false);
        botoes.add(entrar);
        if (!primeiroAcesso) {
            alternar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            alternar.setBackground(Color.WHITE);
            alternar.setForeground(roxo);
            alternar.setFont(fonte);
            alternar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 215, 235)), new EmptyBorder(12, 18, 12, 18)));
            alternar.setFocusPainted(false);
            alternar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            alternar.addActionListener(evento -> {
                cadastrando = !cadastrando;
                montarCampos();
            });
            botoes.add(alternar);
        }
        painel.add(botoes, BorderLayout.SOUTH);
        setContentPane(painel);
        getRootPane().setDefaultButton(entrar);
        setResizable(false);
        montarCampos();
    }

    /** Reaproveita a mesma janela para entrar ou criar uma conta. */
    private void montarCampos() {
        campos.removeAll();
        senha.setText("");
        confirmacao.setText("");
        if (cadastrando) {
            campos.add(new JLabel(primeiroAcesso ? "Primeiro acesso: crie o administrador." : "Crie sua conta de membro."));
            campos.add(new JLabel("Nome"));
            campos.add(nome);
        } else {
            campos.add(new JLabel("Entre com sua conta para acessar o acervo."));
        }
        campos.add(new JLabel("E-mail"));
        campos.add(email);
        campos.add(new JLabel(cadastrando ? "Senha (mínimo de 8 caracteres)" : "Senha"));
        campos.add(senha);
        if (cadastrando) {
            campos.add(new JLabel("Repita a senha"));
            campos.add(confirmacao);
        }
        for (Component componente : campos.getComponents()) {
            componente.setFont(fonte);
            if (componente instanceof JTextField campo) campo.setMargin(new Insets(6, 9, 6, 9));
        }
        entrar.setText(cadastrando ? "Criar conta e entrar" : "Entrar");
        alternar.setText(cadastrando ? "Já tenho conta — entrar" : "Criar uma nova conta");
        campos.revalidate();
        campos.repaint();
        pack();
        setSize(Math.max(510, getWidth()), getHeight());
        setLocationRelativeTo(null);
    }

    private void autenticar() {
        String nomeDigitado = nome.getText();
        String emailDigitado = email.getText();
        char[] senhaDigitada = senha.getPassword();
        char[] repeticao = confirmacao.getPassword();
        boolean novoCadastro = cadastrando;
        senha.setText("");
        confirmacao.setText("");
        entrar.setEnabled(false);
        alternar.setEnabled(false);

        // O cálculo da senha é feito fora da thread da tela.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (primeiroAcesso) {
                    controller.criarAdministrador(nomeDigitado, emailDigitado, senhaDigitada, repeticao);
                } else if (novoCadastro) {
                    controller.criarConta(nomeDigitado, emailDigitado, senhaDigitada, repeticao);
                } else {
                    controller.entrar(emailDigitado, senhaDigitada);
                }
                return null;
            }

            @Override
            protected void done() {
                entrar.setEnabled(true);
                alternar.setEnabled(true);
                try {
                    get();
                    PrincipalFrame principal = new PrincipalFrame(controller);
                    principal.setVisible(true);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    mostrarErro(e.getCause());
                } catch (RuntimeException e) {
                    mostrarErro(e);
                }
            }
        }.execute();
    }

    private void mostrarErro(Throwable erro) {
        String mensagem;
        if (erro instanceof RegraNegocioException) mensagem = erro.getMessage();
        else {
            erro.printStackTrace();
            mensagem = "Não foi possível entrar. Confira o terminal para mais detalhes.";
        }
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }
}
