// Java imports:
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// Nexus's imports:
import aws_connection.S3Provider;
import nex_data.Counter;
import nex_data.DataManipulation;
import nex_data.DataOperations;

// Other imports:
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slack_connection.SlackNotification;
import software.amazon.awssdk.services.s3.S3Client;

public class ExcelSQL {
    private static final Logger logger = LoggerFactory.getLogger(ExcelSQL.class);
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String BUCKET_NAME = "nexusenergybucket"; // Nome do bucket
    private static final String FILE_KEY = "DadosConsumo.xlsx";
    private static int qtdMatrizes;
    private static int qtdFiliais;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Database Environment Variables:
        String dataBase = System.getenv("DB_DATABASE");
        String hostMySQL = System.getenv("DB_HOST");
        String urlMySQL = "jdbc:mysql://%s:3306/%s".formatted(hostMySQL, dataBase);
        String usuario = System.getenv("DB_USER");
        String senha = System.getenv("DB_PASSWORD");

        // Instanciando o aws_connection.S3Provider
        S3Provider s3Provider = new S3Provider();
        S3Client s3Client = s3Provider.getS3Client();
        JSONObject json = new JSONObject();

        LocalDateTime dataAtual = LocalDateTime.now();
        ZoneId fusoHorarioBrasilia = ZoneId.of("America/Sao_Paulo");
        ZonedDateTime dataBrasilia = dataAtual.atZone(ZoneId.systemDefault()).withZoneSameInstant(fusoHorarioBrasilia);
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dataFormatada = dataBrasilia.format(formatador); // Data e Hora Formatada em Horario de Brasilia

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha)) {
            System.out.println(" ");
            logger.info("Conexão com o banco de dados estabelecida.");
            logger.info("Iniciando leitura do arquivo Excel.");
            processarArquivosExcel(conexao, s3Client, dataFormatada);
            System.out.println(" ");
            logger.info("[{}] SUCESSO - Dados inseridos com êxito!", dataFormatada);
            json.put("text",
                    "[" + dataFormatada + "] 🚀 Rotina de Inserção de Dados Finalizada com Sucesso!\n\n" +
                            "📊 Resumo da Execução:\n" +
                            "- Matrizes inseridas: " + qtdMatrizes + "\n" +
                            "- Filiais inseridas: " + qtdFiliais + "\n" +
                            "- Registros de consumo processados: Mais de 3000 dados\n\n" +
                            "🌱 Impacto Ambiental Estimado:\n" +
                            "- Emissão de CO² calculada e compensada com base nos dados.\n" +
                            "- Conversão em árvores necessárias incluída.\n\n" +
                            "✔️ Status: Todos os dados foram inseridos com sucesso no sistema. Até a próxima rotina!"
            );
            SlackNotification.enviarMensagem(json);
        } catch (SQLException e) {
            System.out.println(" ");
            logger.error("[{}] FALHA - Erro de SQL: {}", dataFormatada, e.getMessage());
            json.put("text",
                    "[" + dataFormatada + "] ⚠️ Erro na Rotina de Inserção de Dados\n\n" +
                            "❌ Status: Falha na execução devido a um erro de SQL.\n" +
                            "💡 Detalhes do Erro: `" + e.getMessage() + "`\n\n" +
                            "🚧 Ação Recomendada: Verifique o script SQL e os dados de entrada para corrigir o problema.\n\n" +
                            "⏹️ Resumo:\n" +
                            "- Nenhum dado foi inserido no sistema.\n" +
                            "- Rotina encerrada com erro.\n\n" +
                            "📋 Nota: Acompanhe os logs do sistema para mais informações e reexecute após resolver o problema."
            );
            SlackNotification.enviarMensagem(json);
        } catch (IOException e) {
            System.out.println("");
            logger.error("[{}] FALHA - Erro de I/O: {}", dataFormatada, e.getMessage());
            json.put("text", "[" + dataFormatada + "] 🚨 Rotina de Inserção de Dados Finalizada com Falhas.\n" +
                    "❌ Um erro de I/O foi identificado durante a execução da rotina.\n" +
                    "Detalhes do erro: " + e.getMessage() + "\n" +
                    "⏹️ Verifique os arquivos e a conexão para garantir o funcionamento correto na próxima execução.");
            SlackNotification.enviarMensagem(json);
        }
    }

    private static void processarArquivosExcel(Connection conexao, S3Client s3Client, String dataFormatada) throws IOException, SQLException {
        // Baixando o arquivo do S3
        InputStream leitorExcel = S3Provider.baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);
        Workbook planilha = new XSSFWorkbook(leitorExcel);
        Sheet tabela = planilha.getSheetAt(0);

        // Continua da última linha inserida
        int ultimaLinhaLida = Counter.lerContador();

        System.out.println("Iniciando a inserção de dados.");
        System.out.println("Aguarde, Isso pode durar alguns segundos...");
        System.out.println(" ");

        System.out.println("[" + dataFormatada + "] Inserção de Dados Iniciada");
        System.out.println(" ");

        // Primeiro, insere todos os dados do Excel no banco de dados
        for (int i = 1; i <= tabela.getLastRowNum(); i++) {
            Row linha = tabela.getRow(i);

            System.out.println("INFO - Inserindo linha " + (i) + " do arquivo Excel ao Banco de Dados");

            // Campos da Tabela Matriz
            String nome = DataManipulation.getCellValueAsString(linha.getCell(1));
            String cnpj = DataManipulation.getCellValueAsString(linha.getCell(3));
            Double ativoTotal = DataManipulation.getCellValueAsDouble(linha.getCell(8));

            // Campos da Tabela Filial
            String nomeFilial = DataManipulation.getCellValueAsString(linha.getCell(2));
            String cidade = DataManipulation.getCellValueAsString(linha.getCell(4));
            String uf = DataManipulation.getCellValueAsString(linha.getCell(5));
            String submercado = DataManipulation.getCellValueAsString(linha.getCell(6));

            // Campos da Tabela ConsumoDados
            String mes = DataManipulation.getCellValueAsString(linha.getCell(0));
            Double consumo = DataManipulation.getCellValueAsDouble(linha.getCell(7));
            Double emissaoCO2 = consumo * 81;
            Integer qtdArvores = (int) Math.round(emissaoCO2 / 200);
            LocalDate mesFormatado = DataManipulation.converterYYYYMMParaDate(mes);

            int fkPorte = 0;
            String queryPorte = "SELECT idParametro FROM Parametros WHERE (ativoMin <= ?) AND (ativoMax IS NULL OR ativoMax >= ?)";
            try (PreparedStatement verificarPorte = conexao.prepareStatement(queryPorte)) {
                verificarPorte.setDouble(1, ativoTotal);
                verificarPorte.setDouble(2, ativoTotal);
                try (ResultSet rsPorte = verificarPorte.executeQuery()) {
                    if (rsPorte.next()) {
                        fkPorte = rsPorte.getInt("idParametro");
                    }
                }
            }

            // Verifica se o CNPJ já existe na tabela Matriz para não inserir repitidas Matrizes
            String queryVerificaMatriz = "SELECT idMatriz FROM Matriz WHERE CNPJ = ?";
            int idMatriz = 0;
            try (PreparedStatement verificarMatriz = conexao.prepareStatement(queryVerificaMatriz)) {
                verificarMatriz.setString(1, cnpj);
                ResultSet rsMatriz = verificarMatriz.executeQuery();
                if (rsMatriz.next()) {
                    idMatriz = rsMatriz.getInt("idMatriz");  // Já existe, obtém o idMatriz
                } else {
                    // Insere na tabela Matriz
                    String tabelaMatriz = "INSERT INTO Matriz (nome, CNPJ, ativoTotal) VALUES (?, ?, ?)";
                    try (PreparedStatement executarComandoEmpresa = conexao.prepareStatement(tabelaMatriz, Statement.RETURN_GENERATED_KEYS)) {
                        executarComandoEmpresa.setString(1, nome);
                        executarComandoEmpresa.setString(2, cnpj);
                        executarComandoEmpresa.setDouble(3, ativoTotal);
                        executarComandoEmpresa.executeUpdate();

                        ResultSet rsNovoMatriz = executarComandoEmpresa.getGeneratedKeys();
                        if (rsNovoMatriz.next()) {
                            idMatriz = rsNovoMatriz.getInt(1);
                        }
                    }
                }
            }

            // Verifica se o nome da Filial e a cidade já existem na tabela Filial para não inserir repitidas Filiais
            String queryVerificaFilial = "SELECT idFilial FROM Filial WHERE nome = ? AND cidade = ?";
            int idFilial = 0;
            try (PreparedStatement verificarFilial = conexao.prepareStatement(queryVerificaFilial)) {
                verificarFilial.setString(1, nomeFilial);
                verificarFilial.setString(2, cidade);
                ResultSet rsFilial = verificarFilial.executeQuery();
                if (rsFilial.next()) {
                    idFilial = rsFilial.getInt("idFilial");  // Já existe, obtém o idFilial
                } else {
                    // Insere na tabela Filial, caso não exista
                    String tabelaFilial = "INSERT INTO Filial (nome, cidade, UF, submercado, fkPorte, fkMatriz) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement executarComandoFilial = conexao.prepareStatement(tabelaFilial, Statement.RETURN_GENERATED_KEYS)) {
                        executarComandoFilial.setString(1, nomeFilial);
                        executarComandoFilial.setString(2, cidade);
                        executarComandoFilial.setString(3, uf);
                        executarComandoFilial.setString(4, submercado);
                        executarComandoFilial.setInt(5, fkPorte);
                        executarComandoFilial.setInt(6, idMatriz);
                        executarComandoFilial.executeUpdate();

                        ResultSet rsNovoFilial = executarComandoFilial.getGeneratedKeys();
                        if (rsNovoFilial.next()) {
                            idFilial = rsNovoFilial.getInt(1);  // Obtém o idFilial gerado
                        }
                    }
                }
            }

            // Insere na tabela ConsumoDados
            String tabelaConsumo = "INSERT INTO ConsumoDados (dataReferencia, consumoEnergia, emissaoCO2, qtdArvores, fkFilial) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement executarComandoConsumo = conexao.prepareStatement(tabelaConsumo)) {
                executarComandoConsumo.setDate(1, java.sql.Date.valueOf(mesFormatado));
                executarComandoConsumo.setDouble(2, consumo);
                executarComandoConsumo.setDouble(3, emissaoCO2);
                executarComandoConsumo.setInt(4, qtdArvores);
                executarComandoConsumo.setInt(5, idFilial);
                executarComandoConsumo.executeUpdate();
            }
        }

        // Depois de inserir todos os dados, calcula e insere os dados previstos
        for (int i = 1; i <= tabela.getLastRowNum(); i++) {
            Row linha = tabela.getRow(i);

            String mes = DataManipulation.getCellValueAsString(linha.getCell(0));
            LocalDate mesFormatado = DataManipulation.converterYYYYMMParaDate(mes);
            LocalDateTime mesFormatadoDateTime = mesFormatado.atStartOfDay();

            String nomeFilial = DataManipulation.getCellValueAsString(linha.getCell(2));
            String cidade = DataManipulation.getCellValueAsString(linha.getCell(4));

            // Obtém o idFilial
            String queryVerificaFilial = "SELECT idFilial FROM Filial WHERE nome = ? AND cidade = ?";
            int idFilial = 0;
            try (PreparedStatement verificarFilial = conexao.prepareStatement(queryVerificaFilial)) {
                verificarFilial.setString(1, nomeFilial);
                verificarFilial.setString(2, cidade);
                ResultSet rsFilial = verificarFilial.executeQuery();
                if (rsFilial.next()) {
                    idFilial = rsFilial.getInt("idFilial");
                }
            }

            if (i <= 1069) {
                double consumoMesAnterior = DataOperations.obterConsumoMesAnterior(conexao, mesFormatadoDateTime.minusMonths(1).toLocalDate(), idFilial);
                double consumoDoisMesesAtras = DataOperations.obterConsumoMesAnterior(conexao, mesFormatadoDateTime.minusMonths(2).toLocalDate(), idFilial);


                if (consumoMesAnterior == 0.0 || consumoDoisMesesAtras == 0.0) {
                    System.out.println("Dados faltantes!: " + i);
                } else {
                    double consumoPrevisto = DataManipulation.calcularPrevisaoConsumo(consumoMesAnterior, consumoDoisMesesAtras);
                    System.out.println("Inserindo dados previstos para " + mes + "...");
                    // Prever os próximos 3 meses
                    for (int j = 1; j <= 3; j++) {
                        LocalDate mesFuturo = mesFormatadoDateTime.plusMonths(j).toLocalDate();
                        double consumoPrevistoFuturo = DataManipulation.calcularPrevisaoConsumo(consumoPrevisto, consumoMesAnterior);
                        DataOperations dataOperations = new DataOperations();
                        dataOperations.inserirDadosPrevistos(conexao, mesFuturo, consumoPrevistoFuturo, idFilial);
                        consumoMesAnterior = consumoPrevisto;
                        consumoPrevisto = consumoPrevistoFuturo;
                    }
                }
            }
        }

        String queryInserirUsuario = "INSERT INTO Usuario VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmtInserirUsuario = conexao.prepareStatement(queryInserirUsuario)) {
            stmtInserirUsuario.setString(1, "11187293868"); // CPF
            stmtInserirUsuario.setString(2, "Carlos Miguel"); // Nome
            stmtInserirUsuario.setString(3, "carlosM@gmail.com"); // Email
            stmtInserirUsuario.setString(4, "a5947a636a73901ecff57e08c0057a9ee5795779243f1465d6bbb2f4916df05d"); // Senha criptografada
            stmtInserirUsuario.setLong(5, 11949505111L);
            stmtInserirUsuario.setInt(6, 2);
            stmtInserirUsuario.setInt(7, 1);
            stmtInserirUsuario.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erro ao inserir usuário padrão: {}", e.getMessage());
        }
        String queryInserirUsuario2 = "INSERT INTO Usuario VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmtInserirUsuario2 = conexao.prepareStatement(queryInserirUsuario)) {
            stmtInserirUsuario2.setString(1, "11187293858"); // CPF
            stmtInserirUsuario2.setString(2, "Lucas Santos"); // Nome
            stmtInserirUsuario2.setString(3, "lucasS@gmail.com"); // Email
            stmtInserirUsuario2.setString(4, "a5947a636a73901ecff57e08c0057a9ee5795779243f1465d6bbb2f4916df05d"); // Senha criptografada
            stmtInserirUsuario2.setLong(5, 11949905111L);
            stmtInserirUsuario2.setInt(6, 4);
            stmtInserirUsuario2.setInt(7, 1);
            stmtInserirUsuario2.executeUpdate();

            String queryQtdMatriz = "SELECT COUNT(idMatriz) AS qtdMatriz FROM Matriz";
            String queryQtdFilial = "SELECT COUNT(idFilial) AS qtdFilial FROM Filial";

            try (PreparedStatement stmtQtdMatriz = conexao.prepareStatement(queryQtdMatriz);
                 PreparedStatement stmtQtdFilial = conexao.prepareStatement(queryQtdFilial)) {

                try (ResultSet rsMatriz = stmtQtdMatriz.executeQuery()) {
                    if (rsMatriz.next()) {
                        qtdMatrizes = rsMatriz.getInt("qtdMatriz");
                    }
                }
                try (ResultSet rsFilial = stmtQtdFilial.executeQuery()) {
                    if (rsFilial.next()) {
                        qtdFiliais = rsFilial.getInt("qtdFilial");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Counter.atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
            planilha.close();
        }
    }
}