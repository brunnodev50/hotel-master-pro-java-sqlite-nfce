/*
 * HOTEL MASTER PRO - V44.0 (Fix: Código de Barras Legível)
 * - Correção Crítica: Ajuste na densidade do código de barras (Barcode128) para leitura correta.
 * - Remoção da sobreposição de números na imagem do código de barras.
 * - Ajuste fino de layout para impressoras térmicas 80mm.
 */
package com.mycompany.hotelmaster.java;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.text.MaskFormatter;

public class HotelMasterJava {
    
    // --- CORES & FONTES ---
    public static final Color COR_FUNDO = new Color(240, 242, 245); 
    public static final Color COR_PRIMARIA = new Color(44, 62, 80);
    public static final Color COR_DESTAQUE = new Color(52, 152, 219);
    public static final Color COR_SUCESSO = new Color(39, 174, 96);
    public static final Color COR_ERRO = new Color(192, 57, 43);
    public static final Color COR_BRANCO = Color.WHITE;
    public static final Color COR_TEXTO_PRETO = Color.BLACK; 
    
    public static final java.awt.Font FONT_TEXTO = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16);
    public static final java.awt.Font FONT_BOLD = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16);
    public static final java.awt.Font FONT_TITULO = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24);
    public static final java.awt.Font FONT_TABELA = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14);

    private static int estoqueIdSelecionado = 0; 

    public static void main(String[] args) {
        configurarUI();
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginWindow();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    
    private static void configurarUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Panel.background", COR_FUNDO);
            UIManager.put("OptionPane.background", COR_BRANCO);
            UIManager.put("OptionPane.messageFont", FONT_TEXTO);
            UIManager.put("Button.font", FONT_BOLD);
            UIManager.put("Label.font", FONT_TEXTO);
            UIManager.put("Label.foreground", COR_TEXTO_PRETO);
            UIManager.put("TextField.font", FONT_TEXTO);
            UIManager.put("ComboBox.font", FONT_TEXTO);
            UIManager.put("Table.font", FONT_TABELA);
            UIManager.put("TableHeader.font", FONT_BOLD);
            UIManager.put("TabbedPane.font", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
            UIManager.put("TabbedPane.foreground", COR_TEXTO_PRETO);
            UIManager.put("TabbedPane.selected", COR_BRANCO);
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(10,0,0,0));
            UIManager.put("TabbedPane.tabInsets", new Insets(10, 20, 10, 20));
        } catch (Exception e) {}
    }

    // =======================================================
    // --- HELPERS VISUAIS ---
    // =======================================================
    public static JButton criarBotao(String texto, Color corFundo, Color corTexto) {
        JButton btn = new JButton(texto.toUpperCase()); 
        btn.setBackground(corFundo);
        btn.setForeground(corTexto);
        btn.setFocusPainted(false);
        btn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20)); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(corFundo.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(corFundo); }
        });
        return btn;
    }

    public static void estilizarTabela(JTable table) {
        table.setRowHeight(35); 
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(COR_DESTAQUE); 
        table.setSelectionForeground(COR_TEXTO_PRETO);  
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(200, 200, 200));
        header.setForeground(COR_TEXTO_PRETO);
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
    }
    
    public static JPanel criarPainelCard(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(COR_BRANCO);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200,200,200), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        if(titulo != null) {
            JLabel lbl = new JLabel(titulo);
            lbl.setFont(FONT_TITULO);
            lbl.setForeground(COR_PRIMARIA);
            lbl.setBorder(new EmptyBorder(0, 0, 15, 0));
            p.add(lbl, BorderLayout.NORTH);
        }
        return p;
    }

    // =======================================================
    // --- BANCO DE DADOS ---
    // =======================================================
    public static class BancoDeDados {
        private Connection conn;
        private String dbName = "hotel_db_novo.db";

        public BancoDeDados() throws SQLException {
            conectar();
            criarTabelas();
            verificarEstruturaProdutos();
            popularDados();
        }
        
        private void conectar() throws SQLException {
            String url = "jdbc:sqlite:" + dbName;
            if (conn != null && !conn.isClosed()) return;
            conn = DriverManager.getConnection(url);
        }
        
        public void desconectar() {
            try { if (conn != null && !conn.isClosed()) conn.close(); } catch (Exception e) {}
        }
        
        public void reconectar() throws SQLException {
            desconectar();
            conectar();
        }

        private void criarTabelas() throws SQLException {
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (cpf TEXT PRIMARY KEY, nome TEXT, celular TEXT, email TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS quartos (numero INTEGER PRIMARY KEY, categoria TEXT, preco REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT, marca TEXT, estoque INTEGER, preco REAL, codigo_barras TEXT, ativo INTEGER DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS configuracoes (chave TEXT PRIMARY KEY, valor TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS reservas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "cpf_cliente TEXT, num_quarto INTEGER, " +
                    "data_entrada TEXT, data_saida TEXT, dias INTEGER, valor_diarias REAL, " +
                    "status TEXT, valor_consumo REAL, valor_total REAL, forma_pagamento TEXT, data_pagamento TEXT, " +
                    "FOREIGN KEY(cpf_cliente) REFERENCES clientes(cpf), " +
                    "FOREIGN KEY(num_quarto) REFERENCES quartos(numero))");
            stmt.execute("CREATE TABLE IF NOT EXISTS consumo (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_reserva INTEGER, id_produto INTEGER, quantidade INTEGER, valor_unitario REAL, " +
                    "FOREIGN KEY(id_reserva) REFERENCES reservas(id), " +
                    "FOREIGN KEY(id_produto) REFERENCES produtos(id))");
            stmt.close();
        }
        
        private void verificarEstruturaProdutos() {
            try {
                Statement stmt = conn.createStatement();
                try { stmt.execute("ALTER TABLE produtos ADD COLUMN codigo_barras TEXT"); } catch(Exception e) {}
                try { stmt.execute("ALTER TABLE produtos ADD COLUMN ativo INTEGER DEFAULT 1"); } catch(Exception e) {}
                stmt.close();
            } catch (Exception e) {}
        }

        private void popularDados() throws SQLException {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM quartos");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO quartos VALUES (101,'STANDARD',150.0)");
                stmt.execute("INSERT INTO quartos VALUES (102,'STANDARD',150.0)");
                stmt.execute("INSERT INTO quartos VALUES (201,'LUXO',250.0)");
                stmt.execute("INSERT INTO quartos VALUES (301,'SUITE',400.0)");
            }
            rs.close(); stmt.close();
        }
        
        public void salvarConfig(String chave, String valor) throws SQLException {
            executar("INSERT OR REPLACE INTO configuracoes (chave, valor) VALUES (?, ?)", chave, valor);
        }
        
        public String getConfig(String chave) {
            try {
                List<Object[]> res = consultar("SELECT valor FROM configuracoes WHERE chave=?", chave);
                if (!res.isEmpty()) return (String) res.get(0)[0];
            } catch (Exception e) {}
            return "";
        }

        public void executar(String sql, Object... params) throws SQLException {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
                pstmt.executeUpdate();
            }
        }

        public List<Object[]> consultar(String sql, Object... params) throws SQLException {
            List<Object[]> lista = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int colCount = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        Object[] row = new Object[colCount];
                        for (int i = 0; i < colCount; i++) row[i] = rs.getObject(i + 1);
                        lista.add(row);
                    }
                }
            }
            return lista;
        }
        
        public boolean addConsumo(int idReserva, int idProd, int qtd) {
            try {
                List<Object[]> prods = consultar("SELECT estoque, preco FROM produtos WHERE id=?", idProd);
                if (!prods.isEmpty()) {
                    int estoque = (int) prods.get(0)[0];
                    double preco = (double) prods.get(0)[1];
                    if (estoque < qtd) return false;
                    executar("UPDATE produtos SET estoque=? WHERE id=?", estoque - qtd, idProd);
                    executar("INSERT INTO consumo (id_reserva, id_produto, quantidade, valor_unitario) VALUES (?, ?, ?, ?)", idReserva, idProd, qtd, preco);
                    return true;
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return false;
        }
        
        public String checkout(int idReserva, double consumo, double total, String pgto) {
            String agora = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            try { executar("UPDATE reservas SET status='FINALIZADA', valor_consumo=?, valor_total=?, forma_pagamento=?, data_pagamento=? WHERE id=?", consumo, total, pgto, agora, idReserva); } catch (SQLException e) { e.printStackTrace(); }
            return agora;
        }
    }

    // --- LOGIN WINDOW ---
    public static class LoginWindow extends JFrame {
        BancoDeDados dbTemp;
        
        public LoginWindow() throws SQLException {
            dbTemp = new BancoDeDados();
            String nomeEmpresa = dbTemp.getConfig("RAZAO_SOCIAL");
            if(nomeEmpresa.isEmpty()) nomeEmpresa = "HOTEL MASTER";
            dbTemp.desconectar();

            setTitle("Acesso Restrito - " + nomeEmpresa);
            setSize(450, 400);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            getContentPane().setBackground(COR_FUNDO);
            
            JPanel card = criarPainelCard(null);
            card.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel(nomeEmpresa);
            title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 26));
            title.setForeground(COR_PRIMARIA);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            card.add(title, gbc);
            
            gbc.gridwidth = 1; gbc.gridy = 1;
            card.add(new JLabel("Usuário:"), gbc);
            gbc.gridx = 1;
            JTextField txtUser = new JTextField(15);
            txtUser.setPreferredSize(new Dimension(150, 40));
            card.add(txtUser, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            card.add(new JLabel("Senha:"), gbc);
            gbc.gridx = 1;
            JPasswordField txtPass = new JPasswordField(15);
            txtPass.setPreferredSize(new Dimension(150, 40));
            card.add(txtPass, gbc);

            JButton btnLogin = criarBotao("ACESSAR SISTEMA", COR_DESTAQUE, COR_TEXTO_PRETO);
            
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(30, 10, 10, 10);
            card.add(btnLogin, gbc);

            btnLogin.addActionListener(e -> {
                String u = txtUser.getText();
                String p = new String(txtPass.getPassword());
                if (u.equals("admin") && p.equals("admin")) {
                    try {
                        HotelApp app = new HotelApp();
                        dispose();
                        app.setVisible(true);
                    } catch (Throwable ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Erro fatal: " + ex.getMessage()); }
                } else {
                    JOptionPane.showMessageDialog(this, "Acesso Negado.");
                }
            });

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setBackground(COR_FUNDO);
            wrapper.add(card);
            add(wrapper);
            setVisible(true);
        }
    }

    // --- HOTEL APP (PRINCIPAL) ---
    public static class HotelApp extends JFrame {
        BancoDeDados db;
        JTabbedPane abas;
        String docPath = "documentos";
        String backupPath = "backup-banco";

        String checkoutRoom = null;
        int checkoutRid = 0;
        double checkoutHostingVal = 0.0;
        int checkoutDays = 0;
        double totalCache = 0.0;
        double consumoCache = 0.0;
        
        JLabel lblTotalCheckout, lblInfoCheckout;
        JTable tableCart;
        DefaultTableModel modelCart;
        Runnable refreshDashboardGlobal, refreshClientes, refreshEstoque, refreshCaixa, refreshQuartos;

        public HotelApp() throws SQLException {
            db = new BancoDeDados();
            
            String empresa = db.getConfig("RAZAO_SOCIAL");
            String cnpj = db.getConfig("CNPJ");
            if(empresa.isEmpty()) empresa = "Hotel Master Pro";
            String titulo = empresa + (cnpj.isEmpty() ? "" : " - CNPJ: " + cnpj);
            setTitle(titulo + " - V44.0");
            
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().setBackground(COR_FUNDO);
            
            File dir = new File(docPath); if (!dir.exists()) dir.mkdir();
            File dirBackup = new File(backupPath); if (!dirBackup.exists()) dirBackup.mkdir();

            abas = new JTabbedPane();
            abas.addTab("VISÃO GERAL", initDash());
            abas.addTab("CLIENTES", initClientes());
            abas.addTab("QUARTOS", initQuartos());
            abas.addTab("ESTOQUE", initEstoque());
            abas.addTab("CAIXA", initCaixa());
            abas.addTab("RELATÓRIOS", initRelatorios());
            abas.addTab("CONFIGURAÇÕES", initConfiguracoes());

            abas.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int idx = abas.getSelectedIndex();
                    switch(idx) {
                        case 0: if(refreshDashboardGlobal != null) refreshDashboardGlobal.run(); break;
                        case 1: if(refreshClientes != null) refreshClientes.run(); break;
                        case 2: if(refreshQuartos != null) refreshQuartos.run(); break;
                        case 3: if(refreshEstoque != null) refreshEstoque.run(); break;
                        case 4: if(refreshCaixa != null) refreshCaixa.run(); break;
                    }
                }
            });

            add(abas);
            pack();
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        private DefaultTableModel criarModelo(String[] colunas) {
            return new DefaultTableModel(colunas, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
        }
        
        // --- 7. CONFIGURAÇÕES ---
        private JPanel initConfiguracoes() {
            JPanel panel = new JPanel(new BorderLayout(15, 15)); panel.setBackground(COR_FUNDO); panel.setBorder(new EmptyBorder(15,15,15,15));
            JPanel cardEmpresa = criarPainelCard("Dados da Empresa");
            JPanel gridEmpresa = new JPanel(new GridLayout(2, 2, 10, 10)); gridEmpresa.setBackground(COR_BRANCO);
            JTextField txtRazao = new JTextField(db.getConfig("RAZAO_SOCIAL"));
            MaskFormatter mkCnpj = null;
            try { mkCnpj = new MaskFormatter("##.###.###/####-##"); mkCnpj.setPlaceholderCharacter('_'); } catch(Exception e) {}
            JFormattedTextField txtCnpj = (mkCnpj != null) ? new JFormattedTextField(mkCnpj) : new JFormattedTextField();
            String currentCnpj = db.getConfig("CNPJ");
            if(currentCnpj != null && !currentCnpj.isEmpty()) { txtCnpj.setText(currentCnpj); }
            gridEmpresa.add(new JLabel("Razão Social (Nome no Topo):")); gridEmpresa.add(txtRazao);
            gridEmpresa.add(new JLabel("CNPJ (Para Recibos):")); gridEmpresa.add(txtCnpj);
            JPanel pBtnEmpresa = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pBtnEmpresa.setBackground(COR_BRANCO);
            JButton btnSalvarEmp = criarBotao("Salvar Dados", COR_SUCESSO, COR_TEXTO_PRETO);
            pBtnEmpresa.add(btnSalvarEmp);
            cardEmpresa.add(gridEmpresa, BorderLayout.CENTER); cardEmpresa.add(pBtnEmpresa, BorderLayout.SOUTH);
            
            btnSalvarEmp.addActionListener(e -> {
                try {
                    db.salvarConfig("RAZAO_SOCIAL", txtRazao.getText());
                    db.salvarConfig("CNPJ", txtCnpj.getText());
                    JOptionPane.showMessageDialog(this, "Dados salvos! Reinicie o sistema para ver no título.");
                } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
            });

            JPanel cardBackup = criarPainelCard("Banco de Dados: Backup e Restauração");
            JPanel pBackupActions = new JPanel(new FlowLayout(FlowLayout.LEFT)); pBackupActions.setBackground(COR_BRANCO);
            JButton btnFazerBackup = criarBotao("📥 REALIZAR BACKUP AGORA", COR_DESTAQUE, COR_TEXTO_PRETO);
            JLabel lblLista = new JLabel("Backups Disponíveis para Restaurar:");
            JComboBox<String> cbBackups = new JComboBox<>(); cbBackups.setPreferredSize(new Dimension(300, 35));
            JButton btnRestaurar = criarBotao("📤 RESTAURAR SELECIONADO", COR_ERRO, Color.WHITE); 
            JButton btnAtualizarLista = criarBotao("🔄", Color.LIGHT_GRAY, Color.BLACK);
            
            Runnable listarBackups = () -> {
                cbBackups.removeAllItems();
                File folder = new File(backupPath);
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".db"));
                if(files != null) { for(File f : files) cbBackups.addItem(f.getName()); }
            };
            listarBackups.run();
            pBackupActions.add(btnFazerBackup);
            
            JPanel pRestore = new JPanel(new FlowLayout(FlowLayout.LEFT)); pRestore.setBackground(COR_BRANCO);
            pRestore.setBorder(BorderFactory.createTitledBorder("Área de Restauração"));
            pRestore.add(lblLista); pRestore.add(cbBackups); pRestore.add(btnAtualizarLista); pRestore.add(btnRestaurar);
            
            JPanel pContainerBackup = new JPanel(new BorderLayout()); pContainerBackup.setBackground(COR_BRANCO);
            pContainerBackup.add(pBackupActions, BorderLayout.NORTH); pContainerBackup.add(pRestore, BorderLayout.CENTER);
            cardBackup.add(pContainerBackup, BorderLayout.CENTER);
            
            btnFazerBackup.addActionListener(e -> {
                try {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    File source = new File("hotel_db_novo.db");
                    File dest = new File(backupPath + "/backup_" + timeStamp + ".db");
                    Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(this, "Backup criado com sucesso:\n" + dest.getName());
                    listarBackups.run();
                } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Erro no Backup: " + ex.getMessage()); }
            });
            btnAtualizarLista.addActionListener(e -> listarBackups.run());
            btnRestaurar.addActionListener(e -> {
                String selecionado = (String) cbBackups.getSelectedItem();
                if(selecionado == null) return;
                int confirm = JOptionPane.showConfirmDialog(this, "ATENÇÃO: Isso irá substituir TODOS os dados atuais pelos dados do backup: " + selecionado + "\n\nDeseja continuar?", "Restauração Crítica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if(confirm == JOptionPane.YES_OPTION) {
                    try {
                        db.desconectar(); 
                        File backupFile = new File(backupPath + "/" + selecionado);
                        File currentFile = new File("hotel_db_novo.db");
                        Files.copy(backupFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        JOptionPane.showMessageDialog(this, "Restauração concluída! O sistema será reconectado.");
                        db.reconectar(); refreshDashboardGlobal.run(); 
                    } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Erro Crítico na Restauração: " + ex.getMessage() + "\nReinicie o sistema."); }
                }
            });

            JPanel wrapper = new JPanel(); wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS)); wrapper.setBackground(COR_FUNDO);
            wrapper.add(cardEmpresa); wrapper.add(Box.createVerticalStrut(20)); wrapper.add(cardBackup);
            panel.add(wrapper, BorderLayout.NORTH); return panel;
        }

        // --- 1. CLIENTES ---
        private JPanel initClientes() {
            JPanel panel = new JPanel(new BorderLayout(15, 15)); panel.setBackground(COR_FUNDO); panel.setBorder(new EmptyBorder(15,15,15,15));
            JPanel form = criarPainelCard("Cadastro de Clientes");
            JPanel pFormGrid = new JPanel(new GridLayout(2, 4, 15, 15)); pFormGrid.setBackground(COR_BRANCO);
            
            MaskFormatter mkCpf=null; MaskFormatter mkCel=null; try{mkCpf=new MaskFormatter("###.###.###-##");mkCpf.setPlaceholderCharacter('_');mkCel=new MaskFormatter("(##) #####-####");mkCel.setPlaceholderCharacter('_');}catch(Exception e){}
            JFormattedTextField txtCpf=(mkCpf!=null)?new JFormattedTextField(mkCpf):new JFormattedTextField();
            JFormattedTextField txtCel=(mkCel!=null)?new JFormattedTextField(mkCel):new JFormattedTextField();
            JTextField txtNome = new JTextField(); JTextField txtEmail = new JTextField();
            
            pFormGrid.add(new JLabel("CPF:")); pFormGrid.add(txtCpf);
            pFormGrid.add(new JLabel("Nome:")); pFormGrid.add(txtNome);
            pFormGrid.add(new JLabel("Celular:")); pFormGrid.add(txtCel);
            pFormGrid.add(new JLabel("E-mail:")); pFormGrid.add(txtEmail);
            
            JPanel pActions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pActions.setBackground(COR_BRANCO);
            JButton btnSalvar = criarBotao("Salvar Cliente", COR_SUCESSO, COR_TEXTO_PRETO);
            JButton btnLimpar = criarBotao("Limpar", Color.LIGHT_GRAY, COR_TEXTO_PRETO);
            pActions.add(btnLimpar); pActions.add(btnSalvar);
            form.add(pFormGrid, BorderLayout.CENTER); form.add(pActions, BorderLayout.SOUTH);
            
            JPanel tablePanel = criarPainelCard(null);
            JPanel pBusca = new JPanel(new BorderLayout(10, 10)); pBusca.setBackground(COR_BRANCO);
            JTextField txtBusca = new JTextField(); txtBusca.setPreferredSize(new Dimension(0, 40));
            JButton btnRefresh = criarBotao("Atualizar Lista", COR_DESTAQUE, COR_TEXTO_PRETO);
            pBusca.add(new JLabel("Buscar: "), BorderLayout.WEST); pBusca.add(txtBusca, BorderLayout.CENTER); pBusca.add(btnRefresh, BorderLayout.EAST);
            
            DefaultTableModel model = criarModelo(new String[]{"CPF", "Nome", "Celular", "Email"}); 
            JTable table = new JTable(model); estilizarTabela(table);
            
            tablePanel.add(pBusca, BorderLayout.NORTH); tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

            refreshClientes = () -> {
                model.setRowCount(0); try { String t = "%"+txtBusca.getText().toUpperCase()+"%"; 
                List<Object[]> rows = db.consultar("SELECT * FROM clientes WHERE nome LIKE ? OR cpf LIKE ?", t, t);
                for(Object[] row : rows) model.addRow(new Object[]{row[0], row[1], row[2], row[3]});
                } catch (SQLException e) { e.printStackTrace(); }
            };
            
            btnSalvar.addActionListener(e -> {
                if(txtCpf.getText().contains("___") || txtNome.getText().isEmpty()) { JOptionPane.showMessageDialog(this, "CPF e Nome obrigatórios!"); return; }
                try {
                    db.executar("INSERT OR REPLACE INTO clientes VALUES (?,?,?,?)", txtCpf.getText(), txtNome.getText().toUpperCase(), txtCel.getText(), txtEmail.getText().toLowerCase());
                    JOptionPane.showMessageDialog(this, "Salvo!"); refreshClientes.run(); txtCpf.setValue(null); txtNome.setText(""); txtCel.setValue(null); txtEmail.setText("");
                } catch(SQLException ex) { JOptionPane.showMessageDialog(this, "Erro SQL: " + ex.getMessage()); }
            });
            btnLimpar.addActionListener(e -> { txtCpf.setValue(null); txtNome.setText(""); txtCel.setValue(null); txtEmail.setText(""); });
            btnRefresh.addActionListener(e -> { txtBusca.setText(""); refreshClientes.run(); });
            txtBusca.getDocument().addDocumentListener(new SimpleDocumentListener(refreshClientes));
            table.getSelectionModel().addListSelectionListener(e -> { int row = table.getSelectedRow(); if (row != -1) { txtCpf.setText(model.getValueAt(row, 0).toString()); txtNome.setText(model.getValueAt(row, 1).toString()); txtCel.setText(model.getValueAt(row, 2).toString()); txtEmail.setText(model.getValueAt(row, 3).toString()); } });
            
            refreshClientes.run();
            panel.add(form, BorderLayout.NORTH); panel.add(tablePanel, BorderLayout.CENTER);
            return panel;
        }

        // --- 2. QUARTOS ---
        private JPanel initQuartos() {
            JPanel panel = new JPanel(new BorderLayout(15, 15)); panel.setBackground(COR_FUNDO); panel.setBorder(new EmptyBorder(15,15,15,15));
            JPanel form = criarPainelCard("Gerenciar Quartos");
            JPanel grid = new JPanel(new GridLayout(1, 6, 10, 10)); grid.setBackground(COR_BRANCO);
            JTextField txtNum = new JTextField(); 
            JComboBox<String> cbCat = new JComboBox<>(new String[]{"STANDARD", "LUXO", "SUITE MASTER"});
            JTextField txtPreco = new JTextField();
            grid.add(new JLabel("Número:")); grid.add(txtNum); grid.add(new JLabel("Categoria:")); grid.add(cbCat); grid.add(new JLabel("Preço:")); grid.add(txtPreco);
            JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pBtns.setBackground(COR_BRANCO);
            JButton btnCad = criarBotao("Cadastrar", COR_SUCESSO, COR_TEXTO_PRETO);
            JButton btnAlt = criarBotao("Editar", COR_DESTAQUE, COR_TEXTO_PRETO);
            pBtns.add(btnCad); pBtns.add(btnAlt);
            form.add(grid, BorderLayout.CENTER); form.add(pBtns, BorderLayout.SOUTH);

            JPanel tablePanel = criarPainelCard(null);
            JPanel pBusca = new JPanel(new BorderLayout(10, 10)); pBusca.setBackground(COR_BRANCO);
            JTextField txtBusca = new JTextField(); txtBusca.setPreferredSize(new Dimension(0, 40));
            JButton btnRefresh = criarBotao("Atualizar", COR_DESTAQUE, COR_TEXTO_PRETO);
            pBusca.add(new JLabel("Buscar Quarto: "), BorderLayout.WEST); pBusca.add(txtBusca, BorderLayout.CENTER); pBusca.add(btnRefresh, BorderLayout.EAST);

            DefaultTableModel model = criarModelo(new String[]{"Nº Quarto", "Categoria", "Preço Diária"}); JTable table = new JTable(model); estilizarTabela(table);
            
            refreshQuartos = () -> { 
                model.setRowCount(0); try { 
                    String t = "%"+txtBusca.getText().toUpperCase()+"%"; 
                    List<Object[]> rows = db.consultar("SELECT * FROM quartos WHERE CAST(numero AS TEXT) LIKE ? OR categoria LIKE ?", t, t);
                    for(Object[] row : rows) model.addRow(new Object[]{row[0], row[1], row[2]}); 
                } catch (Exception ex) {} 
            };
            
            txtBusca.getDocument().addDocumentListener(new SimpleDocumentListener(refreshQuartos));
            btnRefresh.addActionListener(e -> { txtBusca.setText(""); refreshQuartos.run(); });

            btnCad.addActionListener(e -> { try { db.executar("INSERT INTO quartos VALUES (?,?,?)", Integer.parseInt(txtNum.getText()), cbCat.getSelectedItem(), Double.parseDouble(txtPreco.getText())); refreshQuartos.run(); JOptionPane.showMessageDialog(this, "Sucesso"); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); } });
            btnAlt.addActionListener(e -> { int row = table.getSelectedRow(); if(row == -1) return; try { db.executar("UPDATE quartos SET numero=?, categoria=?, preco=? WHERE numero=?", Integer.parseInt(txtNum.getText()), cbCat.getSelectedItem(), Double.parseDouble(txtPreco.getText()), Integer.parseInt(model.getValueAt(row, 0).toString())); refreshQuartos.run(); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro"); } });
            table.getSelectionModel().addListSelectionListener(e -> { int row = table.getSelectedRow(); if (row != -1) { txtNum.setText(model.getValueAt(row, 0).toString()); cbCat.setSelectedItem(model.getValueAt(row, 1).toString()); txtPreco.setText(model.getValueAt(row, 2).toString()); } });
            
            refreshQuartos.run(); 
            tablePanel.add(pBusca, BorderLayout.NORTH); tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            panel.add(form, BorderLayout.NORTH); panel.add(tablePanel, BorderLayout.CENTER); return panel;
        }

        // --- 3. ESTOQUE ---
        private JPanel initEstoque() {
            JPanel panel = new JPanel(new BorderLayout(15, 15)); panel.setBackground(COR_FUNDO); panel.setBorder(new EmptyBorder(15,15,15,15));
            JPanel form = criarPainelCard("Controle de Estoque");
            
            JPanel grid = new JPanel(new GridLayout(3, 4, 10, 10)); grid.setBackground(COR_BRANCO);
            JTextField txtNome = new JTextField(); JTextField txtMarca = new JTextField(); 
            JTextField txtQtd = new JTextField(); JTextField txtPreco = new JTextField();
            JTextField txtCodBarra = new JTextField(); 
            JComboBox<String> cbStatus = new JComboBox<>(new String[]{"ATIVO", "INATIVO"});
            
            grid.add(new JLabel("Produto:")); grid.add(txtNome); 
            grid.add(new JLabel("Marca:")); grid.add(txtMarca);
            grid.add(new JLabel("Quantidade:")); grid.add(txtQtd); 
            grid.add(new JLabel("Preço Unit:")); grid.add(txtPreco);
            grid.add(new JLabel("Cód. Barras:")); grid.add(txtCodBarra);
            grid.add(new JLabel("Status:")); grid.add(cbStatus);
            
            JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pBtns.setBackground(COR_BRANCO);
            JButton btnSalvar = criarBotao("SALVAR NOVO ITEM", COR_SUCESSO, COR_TEXTO_PRETO);
            JButton btnLimpar = criarBotao("Limpar", Color.LIGHT_GRAY, COR_TEXTO_PRETO);
            pBtns.add(btnLimpar); pBtns.add(btnSalvar);
            form.add(grid, BorderLayout.CENTER); form.add(pBtns, BorderLayout.SOUTH);
            
            JPanel tablePanel = criarPainelCard(null);
            JPanel pBusca = new JPanel(new BorderLayout(10,10)); pBusca.setBackground(COR_BRANCO);
            JTextField txtBusca = new JTextField(); txtBusca.setPreferredSize(new Dimension(0, 40));
            JButton btnRefresh = criarBotao("Atualizar Estoque", COR_DESTAQUE, COR_TEXTO_PRETO);
            pBusca.add(new JLabel("Buscar: "), BorderLayout.WEST); pBusca.add(txtBusca, BorderLayout.CENTER); pBusca.add(btnRefresh, BorderLayout.EAST);
            
            DefaultTableModel model = criarModelo(new String[]{"ID", "Produto", "Marca", "Qtd", "Preço", "Cód. Barras", "Status"}); 
            JTable table = new JTable(model); estilizarTabela(table);
            
            refreshEstoque = () -> { 
                model.setRowCount(0); try { String term = "%"+txtBusca.getText().toUpperCase()+"%"; 
                List<Object[]> rows = db.consultar("SELECT * FROM produtos WHERE nome LIKE ? OR marca LIKE ? OR codigo_barras LIKE ?", term, term, term);
                for(Object[] row : rows) {
                    String statusStr = (row[6] != null && (int)row[6] == 1) ? "ATIVO" : "INATIVO";
                    model.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], row[5], statusStr});
                }
                } catch (Exception e) {} 
            };
            txtBusca.getDocument().addDocumentListener(new SimpleDocumentListener(refreshEstoque));
            
            btnSalvar.addActionListener(e -> { 
                try { 
                    int ativoVal = cbStatus.getSelectedItem().equals("ATIVO") ? 1 : 0;
                    if (estoqueIdSelecionado == 0) {
                        db.executar("INSERT INTO produtos (nome, marca, estoque, preco, codigo_barras, ativo) VALUES (?,?,?,?,?,?)", 
                            txtNome.getText().toUpperCase(), 
                            txtMarca.getText().toUpperCase(), 
                            Integer.parseInt(txtQtd.getText()), 
                            Double.parseDouble(txtPreco.getText().replace(",",".")),
                            txtCodBarra.getText(),
                            ativoVal);
                        JOptionPane.showMessageDialog(this, "Produto CADASTRADO com sucesso!");
                    } else {
                        db.executar("UPDATE produtos SET nome=?, marca=?, estoque=?, preco=?, codigo_barras=?, ativo=? WHERE id=?", 
                            txtNome.getText().toUpperCase(), 
                            txtMarca.getText().toUpperCase(), 
                            Integer.parseInt(txtQtd.getText()), 
                            Double.parseDouble(txtPreco.getText().replace(",",".")), 
                            txtCodBarra.getText(),
                            ativoVal,
                            estoqueIdSelecionado); 
                        JOptionPane.showMessageDialog(this, "Produto ATUALIZADO com sucesso!");
                    }
                    refreshEstoque.run(); 
                    txtNome.setText(""); txtMarca.setText(""); txtQtd.setText(""); txtPreco.setText(""); txtCodBarra.setText(""); cbStatus.setSelectedIndex(0);
                    estoqueIdSelecionado = 0;
                    btnSalvar.setText("SALVAR NOVO ITEM");
                    btnSalvar.setBackground(COR_SUCESSO);
                    table.clearSelection();
                } catch (Exception ex) { 
                    JOptionPane.showMessageDialog(this, "Erro/Dados Inválidos: " + ex.getMessage()); 
                } 
            });
            
            btnLimpar.addActionListener(e -> { 
                txtNome.setText(""); txtMarca.setText(""); txtQtd.setText(""); txtPreco.setText(""); txtCodBarra.setText("");
                estoqueIdSelecionado = 0;
                btnSalvar.setText("SALVAR NOVO ITEM");
                btnSalvar.setBackground(COR_SUCESSO);
                table.clearSelection();
            });
            
            btnRefresh.addActionListener(e -> { txtBusca.setText(""); refreshEstoque.run(); });
            
            table.getSelectionModel().addListSelectionListener(e -> { 
                int row = table.getSelectedRow(); 
                if(row != -1) { 
                    estoqueIdSelecionado = Integer.parseInt(model.getValueAt(row, 0).toString());
                    txtNome.setText(model.getValueAt(row, 1).toString()); 
                    txtMarca.setText(model.getValueAt(row, 2).toString()); 
                    txtQtd.setText(model.getValueAt(row, 3).toString()); 
                    txtPreco.setText(model.getValueAt(row, 4).toString()); 
                    Object codVal = model.getValueAt(row, 5);
                    txtCodBarra.setText(codVal != null ? codVal.toString() : "");
                    String st = model.getValueAt(row, 6).toString();
                    cbStatus.setSelectedItem(st);
                    
                    btnSalvar.setText("SALVAR ALTERAÇÕES");
                    btnSalvar.setBackground(COR_DESTAQUE);
                } 
            });
            
            refreshEstoque.run();
            tablePanel.add(pBusca, BorderLayout.NORTH); tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            panel.add(form, BorderLayout.NORTH); panel.add(tablePanel, BorderLayout.CENTER); return panel;
        }

        // --- 4. DASHBOARD ---
        private JPanel initDash() {
            DefaultTableModel model = criarModelo(new String[]{"Quarto", "Categoria", "Status", "Hóspede/Preço"});
            JTable table = new JTable(model); estilizarTabela(table);
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    String status = (String) table.getModel().getValueAt(row, 2);
                    if (isSelected) {
                        c.setBackground(COR_PRIMARIA); 
                    } else {
                        if ("OCUPADO".equals(status)) { c.setBackground(new Color(255, 200, 200)); } else { c.setBackground(new Color(200, 255, 200)); }
                    }
                    c.setForeground(COR_TEXTO_PRETO);
                    setBorder(noFocusBorder);
                    return c;
                }
            });
            
            JPanel leftPanel = criarPainelCard("Mapa de Ocupação"); 
            JPanel pBuscaMap = new JPanel(new BorderLayout(5,5)); pBuscaMap.setBackground(COR_BRANCO);
            pBuscaMap.setBorder(new EmptyBorder(0, 0, 10, 0));
            JTextField txtBuscaMap = new JTextField(); txtBuscaMap.setPreferredSize(new Dimension(0, 35));
            pBuscaMap.add(new JLabel("Filtrar Mapa: "), BorderLayout.WEST);
            pBuscaMap.add(txtBuscaMap, BorderLayout.CENTER);
            leftPanel.add(pBuscaMap, BorderLayout.NORTH);
            leftPanel.add(new JScrollPane(table), BorderLayout.CENTER);
            JButton btnRefresh = criarBotao("Atualizar Mapa", COR_DESTAQUE, COR_TEXTO_PRETO);
            leftPanel.add(btnRefresh, BorderLayout.SOUTH);
            
            TableRowSorter<DefaultTableModel> sorterMap = new TableRowSorter<>(model);
            table.setRowSorter(sorterMap);
            txtBuscaMap.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                String text = txtBuscaMap.getText();
                if (text.trim().length() == 0) { sorterMap.setRowFilter(null); } 
                else { sorterMap.setRowFilter(RowFilter.regexFilter("(?i)" + text)); }
            }));

            JPanel rightPanel = criarPainelCard("Realizar Check-In");
            rightPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            
            JLabel lblQuarto = new JLabel("---"); lblQuarto.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24)); lblQuarto.setForeground(COR_DESTAQUE);
            JComboBox<String> cbCli = new JComboBox<>(); cbCli.setPreferredSize(new Dimension(200, 40));
            JSpinner spDias = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1)); spDias.setPreferredSize(new Dimension(80, 40));
            JButton btnCheckin = criarBotao("Confirmar Check-In", COR_SUCESSO, COR_TEXTO_PRETO); btnCheckin.setPreferredSize(new Dimension(0, 50));

            gbc.gridx = 0; gbc.gridy = 0; rightPanel.add(new JLabel("Quarto Selecionado:"), gbc);
            gbc.gridx = 0; gbc.gridy = 1; rightPanel.add(lblQuarto, gbc);
            gbc.gridx = 0; gbc.gridy = 2; rightPanel.add(new JLabel("Cliente:"), gbc);
            gbc.gridx = 0; gbc.gridy = 3; rightPanel.add(cbCli, gbc);
            gbc.gridx = 0; gbc.gridy = 4; rightPanel.add(new JLabel("Dias:"), gbc);
            gbc.gridx = 0; gbc.gridy = 5; rightPanel.add(spDias, gbc);
            gbc.gridx = 0; gbc.gridy = 6; gbc.insets = new Insets(30, 10, 10, 10); rightPanel.add(btnCheckin, gbc);
            gbc.gridy = 7; gbc.weighty = 1.0; rightPanel.add(new JPanel(), gbc);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            split.setDividerLocation(600); split.setBorder(null);

            refreshDashboardGlobal = () -> {
                model.setRowCount(0);
                try {
                    List<Object[]> rowsQ = db.consultar("SELECT * FROM quartos");
                    for(Object[] rowQ : rowsQ) {
                        int num = (int) rowQ[0]; String cat = (String) rowQ[1]; double preco = (double) rowQ[2];
                        List<Object[]> res = db.consultar("SELECT c.nome FROM reservas r JOIN clientes c ON r.cpf_cliente=c.cpf WHERE r.num_quarto=? AND r.status='ATIVA'", num);
                        String status = "LIVRE"; String info = String.format("R$ %.2f", preco);
                        if (!res.isEmpty()) { status = "OCUPADO"; info = (String) res.get(0)[0]; }
                        model.addRow(new Object[]{num, cat, status, info});
                    }
                } catch (SQLException e) {}
                cbCli.removeAllItems();
                try { List<Object[]> rowsC = db.consultar("SELECT cpf, nome FROM clientes"); for(Object[] rowC : rowsC) cbCli.addItem(rowC[1] + " | CPF:" + rowC[0]); } catch (SQLException e) {}
            };

            table.getSelectionModel().addListSelectionListener(e -> {
                if(table.getSelectedRow() != -1) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    lblQuarto.setText("Quarto " + model.getValueAt(modelRow, 0));
                    String st = model.getValueAt(modelRow, 2).toString();
                    btnCheckin.setEnabled(st.equals("LIVRE"));
                    if(st.equals("LIVRE")) btnCheckin.setBackground(COR_SUCESSO); else btnCheckin.setBackground(Color.GRAY);
                }
            });

            btnCheckin.addActionListener(e -> {
                if(lblQuarto.getText().contains("-")) return;
                try {
                    int qNum = Integer.parseInt(lblQuarto.getText().replace("Quarto ", ""));
                    if (cbCli.getSelectedItem() == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente!"); return; }
                    String itemCli = cbCli.getSelectedItem().toString(); String cpf = itemCli.substring(itemCli.lastIndexOf(":") + 1).trim();
                    List<Object[]> prData = db.consultar("SELECT preco FROM quartos WHERE numero=?", qNum);
                    double preco = prData.isEmpty() ? 0 : (double) prData.get(0)[0];
                    int dias = ((Number)spDias.getValue()).intValue(); double total = dias * preco;
                    db.executar("INSERT INTO reservas (cpf_cliente, num_quarto, data_entrada, data_saida, dias, valor_diarias, status, valor_consumo, valor_total) VALUES (?,?,?,?,?,?,'ATIVA',0,0)", cpf, qNum, LocalDate.now().toString(), LocalDate.now().plusDays(dias).toString(), dias, total);
                    JOptionPane.showMessageDialog(this, "Check-in realizado!"); refreshDashboardGlobal.run();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
            });
            btnRefresh.addActionListener(ev -> refreshDashboardGlobal.run());
            
            refreshDashboardGlobal.run();
            JPanel mainPanel = new JPanel(new BorderLayout()); mainPanel.setBorder(new EmptyBorder(15,15,15,15)); mainPanel.setBackground(COR_FUNDO);
            mainPanel.add(split, BorderLayout.CENTER);
            return mainPanel;
        }

        // --- 5. CAIXA ---
        private JPanel initCaixa() {
            JPanel left = criarPainelCard("Contas em Aberto");
            JPanel pBuscaCaixa = new JPanel(new BorderLayout(5,5)); pBuscaCaixa.setBackground(COR_BRANCO);
            pBuscaCaixa.setBorder(new EmptyBorder(0, 0, 10, 0));
            JTextField txtBuscaCaixa = new JTextField(); txtBuscaCaixa.setPreferredSize(new Dimension(0, 35));
            pBuscaCaixa.add(new JLabel("Filtrar Conta: "), BorderLayout.WEST);
            pBuscaCaixa.add(txtBuscaCaixa, BorderLayout.CENTER);
            left.add(pBuscaCaixa, BorderLayout.NORTH);

            DefaultTableModel modelRes = criarModelo(new String[]{"Quarto", "Hóspede", "ID", "Diária", "Dias"});
            JTable tableRes = new JTable(modelRes); estilizarTabela(tableRes);
            TableRowSorter<DefaultTableModel> sorterCaixa = new TableRowSorter<>(modelRes);
            tableRes.setRowSorter(sorterCaixa);
            txtBuscaCaixa.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                String text = txtBuscaCaixa.getText();
                if (text.trim().length() == 0) { sorterCaixa.setRowFilter(null); } 
                else { sorterCaixa.setRowFilter(RowFilter.regexFilter("(?i)" + text)); }
            }));

            JPanel pLeftButtons = new JPanel(new GridLayout(1, 2, 10, 0)); pLeftButtons.setBackground(COR_BRANCO);
            JButton btnAtualizar = criarBotao("Atualizar", COR_DESTAQUE, COR_TEXTO_PRETO);
            JButton btnCancelar = criarBotao("CANCELAR CONTA", COR_ERRO, Color.WHITE);
            pLeftButtons.add(btnAtualizar); pLeftButtons.add(btnCancelar);
            left.add(new JScrollPane(tableRes), BorderLayout.CENTER); 
            left.add(pLeftButtons, BorderLayout.SOUTH);

            JPanel right = criarPainelCard("Detalhes e Pagamento");
            lblInfoCheckout = new JLabel("Selecione um quarto ao lado...", SwingConstants.CENTER); 
            lblInfoCheckout.setFont(FONT_TITULO); lblInfoCheckout.setForeground(COR_PRIMARIA);
            
            JPanel pSearchProd = new JPanel(new BorderLayout(5,5)); pSearchProd.setBackground(COR_BRANCO);
            pSearchProd.setBorder(BorderFactory.createTitledBorder("Adicionar Produto"));
            JTextField txtBuscaProd = new JTextField(); txtBuscaProd.setPreferredSize(new Dimension(0, 35));
            pSearchProd.add(new JLabel("Buscar Produto (Nome/Cód):"), BorderLayout.NORTH);
            pSearchProd.add(txtBuscaProd, BorderLayout.CENTER);
            DefaultTableModel modelBuscaProd = criarModelo(new String[]{"ID", "Produto", "Marca", "Preço", "Cód. Barras"});
            JTable tableBuscaProd = new JTable(modelBuscaProd); estilizarTabela(tableBuscaProd);
            JScrollPane scrollBusca = new JScrollPane(tableBuscaProd); scrollBusca.setPreferredSize(new Dimension(0, 120));
            pSearchProd.add(scrollBusca, BorderLayout.SOUTH);
            
            txtBuscaProd.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                String term = "%" + txtBuscaProd.getText().toUpperCase() + "%";
                modelBuscaProd.setRowCount(0);
                try {
                    List<Object[]> rows = db.consultar("SELECT id, nome, marca, preco, codigo_barras FROM produtos WHERE (nome LIKE ? OR marca LIKE ? OR codigo_barras LIKE ?) AND ativo = 1", term, term, term);
                    for(Object[] r : rows) modelBuscaProd.addRow(new Object[]{r[0], r[1], r[2], String.format("R$ %.2f", r[3]), r[4]});
                } catch(Exception e) {}
            }));

            JPanel pAddControls = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pAddControls.setBackground(COR_BRANCO);
            JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)); spQtd.setPreferredSize(new Dimension(60, 40));
            JButton btnAdd = criarBotao("+ ADICIONAR", COR_SUCESSO, COR_TEXTO_PRETO); 
            pAddControls.add(new JLabel("Qtd:")); pAddControls.add(spQtd); pAddControls.add(btnAdd);
            
            modelCart = criarModelo(new String[]{"ID", "Produto", "Qtd", "Total Item"}); tableCart = new JTable(modelCart); estilizarTabela(tableCart);
            
            JPanel pFooter = new JPanel(new BorderLayout()); pFooter.setBackground(new Color(245, 245, 245)); pFooter.setBorder(new EmptyBorder(15,15,15,15));
            JPanel pPay = new JPanel(new FlowLayout(FlowLayout.RIGHT)); pPay.setOpaque(false);
            JComboBox<String> cbPgto = new JComboBox<>(new String[]{"DINHEIRO", "PIX", "CARTAO CRÉDITO", "CARTAO DÉBITO"}); cbPgto.setPreferredSize(new Dimension(150, 40));
            JButton btnFechar = criarBotao("FECHAR CONTA", COR_SUCESSO, COR_TEXTO_PRETO);
            lblTotalCheckout = new JLabel("R$ 0.00"); lblTotalCheckout.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 32)); lblTotalCheckout.setForeground(COR_SUCESSO);
            pPay.add(new JLabel("Pgto: ")); pPay.add(cbPgto); pPay.add(Box.createHorizontalStrut(20)); pPay.add(btnFechar);
            pFooter.add(lblTotalCheckout, BorderLayout.WEST); pFooter.add(pPay, BorderLayout.EAST);
            
            JPanel topPart = new JPanel(new BorderLayout()); 
            topPart.add(lblInfoCheckout, BorderLayout.NORTH);
            topPart.add(pSearchProd, BorderLayout.CENTER);
            topPart.add(pAddControls, BorderLayout.SOUTH);
            
            JPanel bottomPart = new JPanel(new BorderLayout());
            bottomPart.add(new JScrollPane(tableCart), BorderLayout.CENTER);
            bottomPart.add(pFooter, BorderLayout.SOUTH);
            
            JSplitPane splitRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPart, bottomPart); splitRight.setDividerLocation(300);
            right.add(splitRight, BorderLayout.CENTER);
            JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right); splitMain.setDividerLocation(500); splitMain.setBorder(null);

            refreshCaixa = () -> {
                modelRes.setRowCount(0); 
                try {
                    List<Object[]> rows = db.consultar("SELECT r.id, r.num_quarto, c.nome, r.valor_diarias, r.dias FROM reservas r JOIN clientes c ON r.cpf_cliente=c.cpf WHERE r.status='ATIVA'");
                    for(Object[] r : rows) modelRes.addRow(new Object[]{r[1], r[2], r[0], r[3], r[4]});
                } catch (SQLException e) {}
                modelBuscaProd.setRowCount(0);
                try {
                    List<Object[]> rows = db.consultar("SELECT id, nome, marca, preco, codigo_barras FROM produtos WHERE ativo = 1");
                    for(Object[] r : rows) modelBuscaProd.addRow(new Object[]{r[0], r[1], r[2], String.format("R$ %.2f", r[3]), r[4]});
                } catch(Exception e) {}
            };
            btnAtualizar.addActionListener(e -> refreshCaixa.run());
            
            btnCancelar.addActionListener(e -> {
                int row = tableRes.getSelectedRow();
                if (row == -1) { JOptionPane.showMessageDialog(this, "Selecione uma conta na tabela para cancelar."); return; }
                int confirm = JOptionPane.showConfirmDialog(this, "Deseja realmente remover esta conta e todos os seus itens?\nEsta ação não pode ser desfeita.", "Cancelar Conta", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        int modelRow = tableRes.convertRowIndexToModel(row);
                        int idRes = Integer.parseInt(modelRes.getValueAt(modelRow, 2).toString());
                        db.executar("DELETE FROM consumo WHERE id_reserva=?", idRes);
                        db.executar("DELETE FROM reservas WHERE id=?", idRes);
                        checkoutRid = 0; lblInfoCheckout.setText("Selecione um quarto..."); lblTotalCheckout.setText("R$ 0.00"); modelCart.setRowCount(0);
                        JOptionPane.showMessageDialog(this, "Conta removida com sucesso."); refreshCaixa.run();
                    } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Erro ao remover conta: " + ex.getMessage()); }
                }
            });
            
            tableRes.getSelectionModel().addListSelectionListener(e -> {
                int row = tableRes.getSelectedRow(); if(row == -1) return;
                int modelRow = tableRes.convertRowIndexToModel(row);
                checkoutRoom = modelRes.getValueAt(modelRow, 0).toString(); checkoutRid = Integer.parseInt(modelRes.getValueAt(modelRow, 2).toString());
                checkoutHostingVal = Double.parseDouble(modelRes.getValueAt(modelRow, 3).toString()); checkoutDays = Integer.parseInt(modelRes.getValueAt(modelRow, 4).toString());
                lblInfoCheckout.setText("Quarto " + checkoutRoom + " - " + modelRes.getValueAt(modelRow, 1)); atualizarCarrinho();
            });
            
            btnAdd.addActionListener(e -> {
                if(checkoutRid == 0) { JOptionPane.showMessageDialog(this, "Selecione um quarto primeiro!"); return; }
                int rowProd = tableBuscaProd.getSelectedRow();
                if(rowProd == -1) { JOptionPane.showMessageDialog(this, "Selecione um produto na lista de busca!"); return; }
                try { 
                    int pid = Integer.parseInt(modelBuscaProd.getValueAt(rowProd, 0).toString()); 
                    if(db.addConsumo(checkoutRid, pid, (int)spQtd.getValue())) {
                        atualizarCarrinho();
                        JOptionPane.showMessageDialog(this, "Item adicionado!");
                    } else { JOptionPane.showMessageDialog(this, "Estoque insuficiente!"); }
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            
            btnFechar.addActionListener(e -> {
                if(checkoutRid == 0) return;
                if(JOptionPane.showConfirmDialog(this, "Confirmar Pagamento?", "Fechar Conta", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    String data = db.checkout(checkoutRid, consumoCache, totalCache, (String) cbPgto.getSelectedItem());
                    gerarPDF(checkoutRid, checkoutRoom, checkoutDays, checkoutHostingVal, totalCache, (String) cbPgto.getSelectedItem(), data);
                    refreshCaixa.run(); modelCart.setRowCount(0); lblTotalCheckout.setText("R$ 0.00"); checkoutRid = 0; lblInfoCheckout.setText("Selecione...");
                }
            });
            refreshCaixa.run();
            JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setBorder(new EmptyBorder(15,15,15,15)); wrapper.setBackground(COR_FUNDO);
            wrapper.add(splitMain, BorderLayout.CENTER); return wrapper;
        }
        
        private void atualizarCarrinho() {
            modelCart.setRowCount(0); consumoCache = 0;
            try {
                List<Object[]> rows = db.consultar("SELECT c.id, p.nome, c.quantidade, c.quantidade*c.valor_unitario as total FROM consumo c JOIN produtos p ON c.id_produto=p.id WHERE c.id_reserva=?", checkoutRid);
                for(Object[] r : rows) { double t = (double) r[3]; modelCart.addRow(new Object[]{r[0], r[1], r[2], String.format("R$ %.2f", t)}); consumoCache += t; }
            } catch(SQLException ex) {}
            totalCache = checkoutHostingVal + consumoCache; lblTotalCheckout.setText(String.format("R$ %.2f", totalCache));
        }
        
        // --- GERADOR DE PDF PROFISSIONAL COM BARCODE ---
        private void gerarPDF(int rid, String quarto, int dias, double valHosp, double total, String pgto, String data) {
            try {
                Rectangle pageSize = new Rectangle(226, 1000); // 80mm width
                Document doc = new Document(pageSize, 10, 10, 10, 10);
                String fileName = docPath + "/NFCe_" + rid + ".pdf";
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fileName)); 
                doc.open();

                com.lowagie.text.Font fHead = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
                com.lowagie.text.Font fNorm = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL);
                com.lowagie.text.Font fBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD);
                DecimalFormat df = new DecimalFormat("#,##0.00");
                String line = "------------------------------------------";

                // 1. CABEÇALHO DA EMPRESA
                Paragraph p = new Paragraph(db.getConfig("RAZAO_SOCIAL").isEmpty() ? "HOTEL MASTER PRO" : db.getConfig("RAZAO_SOCIAL"), fHead);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                
                String cnpj = db.getConfig("CNPJ");
                if(cnpj.isEmpty()) cnpj = "00.000.000/0000-00";
                p = new Paragraph("CNPJ: " + cnpj + "  IE: ISENTO", fNorm);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                
                p = new Paragraph("Rua Exemplo, 1000 - Centro, Cidade/UF", fNorm);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                doc.add(new Paragraph(line, fNorm));

                // 2. DADOS DO EXTRATO
                p = new Paragraph("Extrato No. " + String.format("%06d", rid), fBold);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                p = new Paragraph("NFC-e - Não Documento Fiscal", fBold);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                doc.add(new Paragraph(line, fNorm));

                // 3. LISTA DE ITENS
                p = new Paragraph("COD   DESCRIÇÃO   QTD UN   VL UNIT   VL TOTAL", fBold);
                doc.add(p);
                doc.add(new Paragraph(line, fNorm));

                int itemCont = 1;
                String categoriaQuarto = "STD";
                try { List<Object[]> catQ = db.consultar("SELECT q.categoria FROM reservas r JOIN quartos q ON r.num_quarto = q.numero WHERE r.id = ?", rid); if(!catQ.isEmpty()) categoriaQuarto = (String) catQ.get(0)[0]; } catch(Exception e) {}
                
                // Item Hospedagem
                String linhaHosp = String.format("0001  DIARIAS QUARTO %s (%s)\n      %.2f UN x %d  =  R$ %s", quarto, categoriaQuarto, valHosp/dias, dias, df.format(valHosp));
                doc.add(new Paragraph(linhaHosp, fNorm));

                // Itens Produtos
                try { 
                    List<Object[]> prods = db.consultar("SELECT p.id, p.nome, c.quantidade, c.valor_unitario, (c.quantidade * c.valor_unitario), p.codigo_barras FROM consumo c JOIN produtos p ON c.id_produto = p.id WHERE c.id_reserva = ?", rid); 
                    for(Object[] prod : prods) { 
                        String codBar = (String) prod[5]; 
                        if(codBar == null || codBar.isEmpty()) codBar = String.format("%04d", (int)prod[0]);
                        // Limita tamanho do nome
                        String nomeProd = prod[1].toString();
                        if(nomeProd.length() > 20) nomeProd = nomeProd.substring(0, 20);
                        
                        String linhaProd = String.format("%s  %s\n      %d UN x %s  =  R$ %s", 
                            codBar, nomeProd, (int)prod[2], df.format((double)prod[3]), df.format((double)prod[4])); 
                        doc.add(new Paragraph(linhaProd, fNorm)); 
                    } 
                } catch(Exception e) {}
                
                doc.add(new Paragraph(line, fNorm));

                // 4. TOTAIS E PAGAMENTO
                p = new Paragraph("QTD TOTAL DE ITENS: " + itemCont, fNorm); doc.add(p);
                p = new Paragraph("VALOR TOTAL R$ " + df.format(total), fHead); p.setAlignment(Element.ALIGN_RIGHT); doc.add(p);
                p = new Paragraph("FORMA DE PAGAMENTO: " + pgto, fNorm); doc.add(p);
                p = new Paragraph("Valor Pago R$ " + df.format(total), fNorm); doc.add(p);
                p = new Paragraph("Troco R$ 0,00", fNorm); doc.add(p);
                
                // Lei da Transparência (Estimativa 28%)
                doc.add(new Paragraph(line, fNorm));
                double trib = total * 0.28;
                p = new Paragraph("Val Aprox Tributos R$ " + df.format(trib) + " (28,00%)", fBold);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                p = new Paragraph("Fonte: IBPT", fNorm); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                doc.add(new Paragraph(line, fNorm));

                // 5. RODAPÉ E CHAVES
                p = new Paragraph("Emissão Normal", fNorm); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                p = new Paragraph("Ambiente: Produção", fNorm); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                
                String chaveAcesso = "3523 12" + cnpj.replaceAll("\\D","") + " 55 001 000 000 " + String.format("%09d", rid) + " 1234 5678";
                p = new Paragraph("CHAVE DE ACESSO", fBold); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                p = new Paragraph(chaveAcesso, fNorm); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                
                // --- CÓDIGO DE BARRAS LINEAR (BARCODE 128) ---
                try {
                    // Remove espaços para o código de barras
                    String chaveLimpa = chaveAcesso.replaceAll(" ", "");
                    Barcode128 code128 = new Barcode128();
                    code128.setCode(chaveLimpa);
                    code128.setCodeType(Barcode128.CODE128);
                    code128.setX(0.75f); // Densidade da barra
                    code128.setBarHeight(40f);
                    code128.setFont(null); // Remove texto automático
                    com.lowagie.text.Image img128 = code128.createImageWithBarcode(writer.getDirectContent(), null, null);
                    img128.setAlignment(Element.ALIGN_CENTER);
                    doc.add(new Paragraph(" "));
                    doc.add(img128);
                } catch(Exception e) { System.out.println("Erro Barcode: " + e); }

                doc.add(new Paragraph(" "));
                
                // CONSUMIDOR
                p = new Paragraph("CONSUMIDOR NÃO IDENTIFICADO", fNorm); p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                
                // QR CODE (Mantido API pois funciona bem)
                try {
                    String conteudoQR = "https://www.sefaz.gov.br/nfce/qrcode?chNFe=" + chaveAcesso.replace(" ", "") + "&nVersao=100&tpAmb=1&dhEmi=" + data.replace(" ", "T") + "&vNF=" + df.format(total).replace(",", ".");
                    String urlApi = "https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=" + URLEncoder.encode(conteudoQR, StandardCharsets.UTF_8.toString());
                    com.lowagie.text.Image imgQR = com.lowagie.text.Image.getInstance(new URL(urlApi));
                    imgQR.setAlignment(Element.ALIGN_CENTER);
                    doc.add(new Paragraph("Consulta via Leitor QR Code", fNorm));
                    doc.add(imgQR);
                } catch (Exception ex) {
                    doc.add(new Paragraph("[QR Code indisponível]", fNorm));
                }
                
                p = new Paragraph("Protocolo de Autorização: 13523000" + rid, fNorm);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);
                p = new Paragraph("Data de Autorização: " + data, fNorm);
                p.setAlignment(Element.ALIGN_CENTER); doc.add(p);

                doc.close();
                Desktop.getDesktop().open(new File(fileName));
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + e.getMessage());
            }
        }

        // --- 6. RELATÓRIOS ---
        private JPanel initRelatorios() {
            JPanel panel = new JPanel(new BorderLayout(15, 15)); panel.setBackground(COR_FUNDO); panel.setBorder(new EmptyBorder(15,15,15,15));
            DefaultTableModel model = criarModelo(new String[]{"ID", "Data Pgto", "Quarto", "Dias", "Cliente", "Valor Total", "Forma Pgto"}); 
            JTable table = new JTable(model); estilizarTabela(table);
            
            JPanel pHeader = criarPainelCard("Relatório Financeiro");
            JPanel pControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10)); pControls.setBackground(COR_BRANCO);
            
            MaskFormatter mk = null; try { mk = new MaskFormatter("####-##-##"); mk.setPlaceholderCharacter('_'); } catch(Exception e){}
            JFormattedTextField txtDe = (mk != null) ? new JFormattedTextField(mk) : new JFormattedTextField(); txtDe.setPreferredSize(new Dimension(100, 40));
            JFormattedTextField txtAte = (mk != null) ? new JFormattedTextField(mk) : new JFormattedTextField(); txtAte.setPreferredSize(new Dimension(100, 40));
            String hoje = LocalDate.now().toString(); txtDe.setText(hoje.substring(0, 8) + "01"); txtAte.setText(hoje);
            
            JButton btnFiltrar = criarBotao("Filtrar Data", COR_PRIMARIA, COR_TEXTO_PRETO);
            JButton btnTodos = criarBotao("Ver Tudo", COR_DESTAQUE, COR_TEXTO_PRETO);
            JButton btnVer = criarBotao("Reimprimir NFC-e", Color.LIGHT_GRAY, COR_TEXTO_PRETO);
            
            pControls.add(new JLabel("De:")); pControls.add(txtDe); pControls.add(new JLabel("Até:")); pControls.add(txtAte);
            pControls.add(btnFiltrar); pControls.add(btnTodos); pControls.add(btnVer);
            pHeader.add(pControls, BorderLayout.CENTER);
            
            btnFiltrar.addActionListener(e -> {
                model.setRowCount(0);
                try { 
                    List<Object[]> rows = db.consultar("SELECT r.id, r.data_pagamento, r.num_quarto, r.dias, c.nome, r.valor_total, r.forma_pagamento FROM reservas r JOIN clientes c ON r.cpf_cliente=c.cpf WHERE r.status='FINALIZADA' AND substr(data_pagamento, 1, 10) BETWEEN ? AND ?", txtDe.getText(), txtAte.getText());
                    for(Object[] r : rows) model.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4], String.format("R$ %.2f", r[5]), r[6]});
                } catch(Exception ex) {}
            });
            btnTodos.addActionListener(e -> {
                model.setRowCount(0);
                try { 
                    List<Object[]> rows = db.consultar("SELECT r.id, r.data_pagamento, r.num_quarto, r.dias, c.nome, r.valor_total, r.forma_pagamento FROM reservas r JOIN clientes c ON r.cpf_cliente=c.cpf WHERE r.status='FINALIZADA'");
                    for(Object[] r : rows) model.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4], String.format("R$ %.2f", r[5]), r[6]});
                } catch(Exception ex) {}
            });
            
            btnVer.addActionListener(e -> { 
                int row = table.getSelectedRow(); 
                if(row != -1) { 
                    try { 
                         int rid = Integer.parseInt(table.getValueAt(row, 0).toString());
                         String data = table.getValueAt(row, 1).toString();
                         String quarto = table.getValueAt(row, 2).toString();
                         int dias = Integer.parseInt(table.getValueAt(row, 3).toString());
                         String valStr = table.getValueAt(row, 5).toString().replace("R$ ", "").replace(",", ".");
                         double total = Double.parseDouble(valStr);
                         String pgto = table.getValueAt(row, 6).toString();
                         List<Object[]> resData = db.consultar("SELECT valor_diarias FROM reservas WHERE id=?", rid);
                         double valHosp = 0;
                         if(!resData.isEmpty()) valHosp = (double) resData.get(0)[0];
                         gerarPDF(rid, quarto, dias, valHosp, total, pgto, data);
                    } catch(Exception ex) { 
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Erro ao reimprimir: " + ex.getMessage()); 
                    } 
                } 
            });

            panel.add(pHeader, BorderLayout.NORTH); panel.add(new JScrollPane(table), BorderLayout.CENTER);
            return panel;
        }
    }

    static class SimpleDocumentListener implements DocumentListener {
        Runnable r; public SimpleDocumentListener(Runnable r) { this.r = r; }
        public void insertUpdate(DocumentEvent e) { r.run(); } public void removeUpdate(DocumentEvent e) { r.run(); } public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}