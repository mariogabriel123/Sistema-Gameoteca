package br.com.gameoteca.telas;

import br.com.gameoteca.controle.GameotecaController;
import br.com.gameoteca.erros.RegraNegocioException;
import br.com.gameoteca.modelo.Emprestimo;
import br.com.gameoteca.modelo.Jogo;
import br.com.gameoteca.modelo.Usuario;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Uma janela com três abas; os formulários usam diálogos simples do Swing. */
public class PrincipalFrame extends JFrame {
    // Aparência básica do Swing; as regras continuam no controller e no serviço.
    private final Color roxo = new Color(101, 66, 212);
    private final Color fundo = new Color(246, 245, 251);
    private final Font fonte = new Font("SansSerif", Font.PLAIN, 14);
    private final GameotecaController controller;
    private final boolean administrador;
    private final JLabel conta = new JLabel();
    private final JLabel resumo = new JLabel();
    private final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private List<Jogo> jogos = new ArrayList<>();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Emprestimo> emprestimos = new ArrayList<>();

    private final DefaultTableModel modeloJogos = modelo(new String[]{
            "Título", "Gênero", "Plataforma", "Modalidade", "Total", "Disponíveis", "Ativo"});
    private final DefaultTableModel modeloUsuarios = modelo(new String[]{"Nome", "E-mail", "Perfil", "Ativo"});
    private final DefaultTableModel modeloEmprestimos = modelo(new String[]{
            "Nº", "Usuário", "Jogo", "Retirada", "Prazo", "Devolução", "Situação"});
    private final JTable tabelaJogos = new JTable(modeloJogos);
    private final JTable tabelaUsuarios = new JTable(modeloUsuarios);
    private final JTable tabelaEmprestimos = new JTable(modeloEmprestimos);

    public PrincipalFrame(GameotecaController controller) {
        super("Gameoteca");
        this.controller = controller;
        administrador = controller.usuarioAtual().isAdministrador();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 18));
        raiz.setBackground(fundo);
        raiz.setBorder(BorderFactory.createEmptyBorder(22, 22, 16, 22));
        JPanel cabecalho = new JPanel(new BorderLayout(15, 0));
        cabecalho.setOpaque(false);
        JPanel marca = new JPanel(new BorderLayout(0, 5));
        marca.setOpaque(false);
        JLabel titulo = new JLabel("Gameoteca");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 29));
        titulo.setForeground(roxo);
        JLabel descricao = new JLabel("Seu acervo de jogos para consoles e PC");
        descricao.setFont(fonte);
        descricao.setForeground(new Color(100, 105, 125));
        marca.add(titulo, BorderLayout.NORTH);
        marca.add(descricao, BorderLayout.SOUTH);
        cabecalho.add(marca, BorderLayout.WEST);
        JPanel acesso = new JPanel(new BorderLayout(0, 10));
        acesso.setOpaque(false);
        conta.setFont(fonte);
        conta.setHorizontalAlignment(SwingConstants.RIGHT);
        acesso.add(conta, BorderLayout.NORTH);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        adicionarBotao(botoes, "Atualizar", () -> { });
        JButton sair = new JButton("Sair");
        estilizarBotao(sair);
        sair.addActionListener(evento -> {
            controller.sair();
            new LoginFrame(controller).setVisible(true);
            dispose();
        });
        botoes.add(sair);
        acesso.add(botoes, BorderLayout.SOUTH);
        cabecalho.add(acesso, BorderLayout.EAST);
        raiz.add(cabecalho, BorderLayout.NORTH);

        prepararTabela(tabelaJogos);
        prepararTabela(tabelaUsuarios);
        prepararTabela(tabelaEmprestimos);
        JTabbedPane abas = new JTabbedPane();
        abas.setFont(fonte.deriveFont(Font.BOLD));
        abas.setBackground(Color.WHITE);
        abas.setForeground(new Color(45, 40, 65));
        abas.addTab("Jogos", abaJogos());
        abas.addTab("Empréstimos", abaEmprestimos());
        if (administrador) abas.addTab("Usuários", abaUsuarios());
        raiz.add(abas, BorderLayout.CENTER);
        resumo.setFont(fonte.deriveFont(12f));
        resumo.setForeground(new Color(100, 105, 125));
        raiz.add(resumo, BorderLayout.SOUTH);
        tabelaJogos.getColumnModel().getColumn(0).setPreferredWidth(250);
        tabelaJogos.getColumnModel().getColumn(4).setPreferredWidth(55);
        tabelaJogos.getColumnModel().getColumn(5).setPreferredWidth(90);
        tabelaJogos.getColumnModel().getColumn(6).setPreferredWidth(55);
        tabelaEmprestimos.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabelaEmprestimos.getColumnModel().getColumn(2).setPreferredWidth(230);
        setContentPane(raiz);
        setSize(1180, 740);
        setMinimumSize(new Dimension(1080, 640));
        setLocationRelativeTo(null);
        atualizarTabelas();
    }

    private JPanel abaJogos() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adicionarBusca(barra, tabelaJogos);
        if (administrador) {
            adicionarBotao(barra, "Novo jogo", () -> formularioJogo(null));
            adicionarBotao(barra, "Editar", () -> formularioJogo(jogoSelecionado()));
            adicionarBotao(barra, "Excluir", () -> {
                Jogo jogo = jogoSelecionado();
                if (confirmar("Excluir o jogo " + jogo.getTitulo() + "?")) controller.excluirJogo(jogo.getId());
            });
        }
        adicionarBotao(barra, "Solicitar jogo", () -> formularioEmprestimo(jogoSelecionado()));
        return aba(barra, tabelaJogos, "Mídia física: 7 dias. Acesso local: encerramento no mesmo dia.");
    }

    private JPanel abaUsuarios() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adicionarBusca(barra, tabelaUsuarios);
        adicionarBotao(barra, "Novo usuário", () -> formularioUsuario(null));
        adicionarBotao(barra, "Editar", () -> formularioUsuario(usuarioSelecionado()));
        adicionarBotao(barra, "Excluir", () -> {
            Usuario usuario = usuarioSelecionado();
            if (confirmar("Excluir o usuário " + usuario.getNome() + "?")) controller.excluirUsuario(usuario.getId());
        });
        return aba(barra, tabelaUsuarios, "Cadastros com histórico podem ser desativados para preservar os empréstimos.");
    }

    private JPanel abaEmprestimos() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adicionarBotao(barra, "Novo empréstimo", () -> formularioEmprestimo(null));
        if (administrador) {
            adicionarBotao(barra, "Confirmar devolução", () -> {
                int linha = tabelaEmprestimos.getSelectedRow();
                if (linha < 0) throw new RegraNegocioException("Selecione um empréstimo.");
                Emprestimo emprestimo = emprestimos.get(linha);
                if (confirmar("Confirmar a devolução de " + emprestimo.getJogoTitulo() + "?"))
                    controller.devolver(emprestimo.getId());
            });
            adicionarBotao(barra, "Excluir registro", () -> {
                int linha = tabelaEmprestimos.getSelectedRow();
                if (linha < 0) throw new RegraNegocioException("Selecione um empréstimo devolvido.");
                Emprestimo emprestimo = emprestimos.get(linha);
                if (emprestimo.isAberto())
                    throw new RegraNegocioException("Confirme a devolução antes de excluir este registro.");
                if (confirmar("Excluir permanentemente o registro de " + emprestimo.getJogoTitulo() + "?"))
                    controller.excluirHistorico(emprestimo.getId());
            });
            adicionarBotao(barra, "Limpar histórico", () -> {
                if (confirmar("Excluir permanentemente todos os empréstimos já devolvidos?\nOs empréstimos em andamento serão mantidos.")) {
                    int excluidos = controller.excluirHistorico(null);
                    JOptionPane.showMessageDialog(this, excluidos + " registro(s) removido(s) do histórico.");
                }
            });
        }
        return aba(barra, tabelaEmprestimos, "Limite: 3 jogos em andamento por usuário. Atrasos impedem novas retiradas.");
    }

    private void formularioJogo(Jogo jogo) {
        JTextField titulo = new JTextField(jogo == null ? "" : jogo.getTitulo(), 24);
        JTextField genero = new JTextField(jogo == null ? "" : jogo.getGenero());
        JComboBox<String> plataforma = new JComboBox<>(new String[]{"PC", "PS4", "PS5", "XBOX_ONE", "XBOX_SERIES", "SWITCH"});
        plataforma.setSelectedItem(jogo == null ? "PS5" : jogo.getPlataforma());
        JComboBox<String> modalidade = new JComboBox<>(new String[]{"Mídia física", "Acesso local"});
        modalidade.setSelectedIndex(jogo == null || jogo.getModalidade().equals("FISICO") ? 0 : 1);
        JSpinner quantidade = new JSpinner(new SpinnerNumberModel(jogo == null ? 1 : jogo.getQuantidade(), 1, 999, 1));
        JCheckBox ativo = new JCheckBox("Disponível no catálogo", jogo == null || jogo.isAtivo());
        JPanel campos = new JPanel(new GridLayout(0, 2, 10, 12));
        campo(campos, "Título", titulo);
        campo(campos, "Gênero", genero);
        campo(campos, "Plataforma", plataforma);
        campo(campos, "Modalidade", modalidade);
        campo(campos, "Quantidade total", quantidade);
        campo(campos, "Situação", ativo);
        confirmarFormulario(jogo == null ? "Novo jogo" : "Editar jogo", campos, () -> {
            try { quantidade.commitEdit(); }
            catch (java.text.ParseException e) { throw new RegraNegocioException("Informe uma quantidade inteira entre 1 e 999."); }
            controller.salvarJogo(jogo == null ? null : jogo.getId(), titulo.getText(), genero.getText(),
                    (String) plataforma.getSelectedItem(), modalidade.getSelectedIndex() == 0,
                    (Integer) quantidade.getValue(), ativo.isSelected());
        });
    }

    private void formularioUsuario(Usuario usuario) {
        JTextField nome = new JTextField(usuario == null ? "" : usuario.getNome(), 24);
        JTextField email = new JTextField(usuario == null ? "" : usuario.getEmail());
        JPasswordField senha = new JPasswordField();
        JCheckBox admin = new JCheckBox("Administrador", usuario != null && usuario.isAdministrador());
        JCheckBox ativo = new JCheckBox("Conta ativa", usuario == null || usuario.isAtivo());
        JPanel campos = new JPanel(new GridLayout(0, 2, 10, 12));
        campo(campos, "Nome", nome);
        campo(campos, "E-mail", email);
        campo(campos, usuario == null ? "Senha (mínimo de 8 caracteres)" : "Senha (em branco mantém a atual)", senha);
        campo(campos, "Perfil", admin);
        campo(campos, "Situação", ativo);
        confirmarFormulario(usuario == null ? "Novo usuário" : "Editar usuário", campos, () -> {
            char[] digitada = senha.getPassword();
            senha.setText("");
            controller.salvarUsuario(usuario == null ? null : usuario.getId(), nome.getText(), email.getText(),
                    admin.isSelected(), ativo.isSelected(), digitada);
        });
    }

    private void formularioEmprestimo(Jogo selecionado) {
        JComboBox<Usuario> usuario = new JComboBox<>();
        for (Usuario item : controller.usuariosParaEmprestimo()) usuario.addItem(item);
        JComboBox<Jogo> jogo = new JComboBox<>();
        for (Jogo item : controller.listarJogos()) {
            if (item.isAtivo() && item.getDisponiveis() > 0) {
                jogo.addItem(item);
                if (selecionado != null && selecionado.getId().equals(item.getId())) jogo.setSelectedItem(item);
            }
        }
        if (jogo.getItemCount() == 0) throw new RegraNegocioException("Não há jogos disponíveis. Cadastre um jogo ou confirme as devoluções.");
        if (selecionado != null && (!selecionado.isAtivo() || selecionado.getDisponiveis() <= 0))
            throw new RegraNegocioException("Selecione um jogo ativo com unidades disponíveis.");
        JLabel prazo = new JLabel();
        Runnable atualizarPrazo = () -> {
            Jogo escolhido = (Jogo) jogo.getSelectedItem();
            if (escolhido != null) prazo.setText(LocalDate.now().plusDays(escolhido.getPrazoDias()).format(formatoData));
        };
        jogo.addActionListener(evento -> atualizarPrazo.run());
        atualizarPrazo.run();
        JPanel campos = new JPanel(new GridLayout(0, 2, 10, 12));
        campo(campos, "Usuário", usuario);
        campo(campos, "Jogo", jogo);
        campo(campos, "Devolução prevista", prazo);
        confirmarFormulario("Novo empréstimo", campos, () -> {
            Usuario escolhido = (Usuario) usuario.getSelectedItem();
            Jogo escolhidoJogo = (Jogo) jogo.getSelectedItem();
            if (escolhido == null || escolhidoJogo == null) throw new RegraNegocioException("Selecione o usuário e o jogo.");
            controller.emprestar(escolhido.getId(), escolhidoJogo.getId());
        });
    }

    private void atualizarTabelas() {
        Usuario atual = controller.usuarioAtual();
        conta.setText(atual.getNome() + (atual.isAdministrador() ? " - Administrador" : " - Membro"));
        jogos = controller.listarJogos();
        modeloJogos.setRowCount(0);
        for (Jogo jogo : jogos) {
            modeloJogos.addRow(new Object[]{jogo.getTitulo(), jogo.getGenero(), jogo.getPlataforma(),
                    jogo.getModalidade().equals("FISICO") ? "Mídia física" : "Acesso local",
                    jogo.getQuantidade(), jogo.getDisponiveis(), jogo.isAtivo() ? "Sim" : "Não"});
        }
        emprestimos = controller.listarEmprestimos();
        modeloEmprestimos.setRowCount(0);
        for (Emprestimo emprestimo : emprestimos) {
            modeloEmprestimos.addRow(new Object[]{emprestimo.getId(), emprestimo.getUsuarioNome(),
                    emprestimo.getJogoTitulo(), data(emprestimo.getRetirada()), data(emprestimo.getPrevista()),
                    data(emprestimo.getDevolucao()), emprestimo.getSituacao()});
        }
        if (administrador) {
            usuarios = controller.listarUsuarios();
            modeloUsuarios.setRowCount(0);
            for (Usuario usuario : usuarios) {
                modeloUsuarios.addRow(new Object[]{usuario.getNome(), usuario.getEmail(),
                        usuario.isAdministrador() ? "Administrador" : "Membro", usuario.isAtivo() ? "Sim" : "Não"});
            }
        }
        resumo.setText(jogos.size() + " jogos no catálogo  |  " + emprestimos.size()
                + " registros de empréstimo  |  Selecione uma linha para usar as ações.");
    }

    private Jogo jogoSelecionado() {
        int linha = tabelaJogos.getSelectedRow();
        if (linha < 0) throw new RegraNegocioException("Selecione um jogo na tabela.");
        return jogos.get(tabelaJogos.convertRowIndexToModel(linha));
    }

    private Usuario usuarioSelecionado() {
        int linha = tabelaUsuarios.getSelectedRow();
        if (linha < 0) throw new RegraNegocioException("Selecione um usuário na tabela.");
        return usuarios.get(tabelaUsuarios.convertRowIndexToModel(linha));
    }

    // Abaixo ficam somente funções auxiliares de apresentação.
    private DefaultTableModel modelo(String[] colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) { return false; }
            @Override
            public Class<?> getColumnClass(int coluna) {
                if (getRowCount() == 0 || getValueAt(0, coluna) == null) return Object.class;
                return getValueAt(0, coluna).getClass();
            }
        };
    }

    private void prepararTabela(JTable tabela) {
        tabela.setFont(fonte);
        tabela.setRowHeight(38);
        tabela.setIntercellSpacing(new Dimension(12, 1));
        tabela.setBackground(Color.WHITE);
        tabela.setForeground(new Color(40, 40, 55));
        tabela.setSelectionBackground(new Color(230, 221, 255));
        tabela.setSelectionForeground(new Color(50, 30, 100));
        tabela.setGridColor(new Color(232, 230, 240));
        tabela.setShowVerticalLines(false);
        tabela.getTableHeader().setFont(fonte.deriveFont(Font.BOLD));
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
    }

    private JPanel aba(JPanel barra, JTable tabela, String orientacao) {
        JPanel painel = new JPanel(new BorderLayout(0, 14));
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        barra.setOpaque(false);
        painel.add(barra, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(225, 222, 235)));
        painel.add(rolagem, BorderLayout.CENTER);
        JLabel ajuda = new JLabel(orientacao);
        ajuda.setFont(fonte.deriveFont(12f));
        ajuda.setForeground(new Color(100, 105, 125));
        painel.add(ajuda, BorderLayout.SOUTH);
        return painel;
    }

    private void adicionarBotao(JPanel barra, String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        estilizarBotao(botao);
        botao.addActionListener(evento -> {
            try {
                acao.run();
                atualizarTabelas();
            } catch (RuntimeException erro) { mostrarErro(erro); }
        });
        barra.add(botao);
    }

    private void estilizarBotao(JButton botao) {
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setFont(fonte.deriveFont(13f));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setBackground(Color.WHITE);
        botao.setForeground(new Color(55, 45, 80));
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 215, 232)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        String texto = botao.getText();
        if (texto.startsWith("Novo") || texto.startsWith("Solicitar") || texto.startsWith("Confirmar")) {
            botao.setBackground(roxo);
            botao.setForeground(Color.WHITE);
        } else if (texto.startsWith("Excluir") || texto.equals("Limpar histórico")) {
            botao.setForeground(new Color(170, 45, 55));
        }
    }

    private void adicionarBusca(JPanel barra, JTable tabela) {
        JTextField busca = new JTextField(16);
        busca.setFont(fonte);
        busca.setMargin(new Insets(8, 9, 8, 9));
        busca.setToolTipText("Digite para pesquisar os registros da tabela.");
        TableRowSorter<DefaultTableModel> filtro = new TableRowSorter<>((DefaultTableModel) tabela.getModel());
        tabela.setRowSorter(filtro);
        busca.getDocument().addDocumentListener(new DocumentListener() {
            private void atualizar() {
                filtro.setRowFilter(RowFilter.regexFilter("(?iu)" + Pattern.quote(busca.getText().trim())));
            }
            @Override public void insertUpdate(DocumentEvent evento) { atualizar(); }
            @Override public void removeUpdate(DocumentEvent evento) { atualizar(); }
            @Override public void changedUpdate(DocumentEvent evento) { atualizar(); }
        });
        JLabel rotulo = new JLabel("Buscar:");
        rotulo.setFont(fonte);
        barra.add(rotulo);
        barra.add(busca);
    }

    private void campo(JPanel painel, String rotulo, JComponent componente) {
        JLabel label = new JLabel(rotulo);
        label.setFont(fonte);
        componente.setFont(fonte);
        if (componente instanceof JTextField texto) texto.setMargin(new Insets(7, 9, 7, 9));
        painel.add(label);
        painel.add(componente);
    }

    private void confirmarFormulario(String titulo, JPanel campos, Runnable salvar) {
        campos.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        while (JOptionPane.showConfirmDialog(this, campos, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                salvar.run();
                return;
            } catch (RuntimeException erro) { mostrarErro(erro); }
        }
    }

    private boolean confirmar(String mensagem) {
        return JOptionPane.showConfirmDialog(this, mensagem, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private String data(LocalDate data) {
        return data == null ? "" : data.format(formatoData);
    }

    private void mostrarErro(RuntimeException erro) {
        String mensagem;
        if (erro instanceof RegraNegocioException) mensagem = erro.getMessage();
        else {
            erro.printStackTrace();
            mensagem = "Não foi possível concluir a operação. Confira o terminal para mais detalhes.";
        }
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }
}
