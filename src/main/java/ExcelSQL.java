// Java imports:
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Nexus's imports:
import aws_connection.S3Provider;
import nex_data.Counter;
import nex_data.DataManipulation;
import nex_data.DataOperations;

// Other imports:
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;



public class ExcelSQL {
    private static final Logger logger = LoggerFactory.getLogger(ExcelSQL.class);
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String BUCKET_NAME = "nexusenergybucket"; // Nome do bucket
    private static final String FILE_KEY = "DadosConsumo.xlsx";    // Nome do arquivo no bucket


    public static void main(String[] args) {
        // Database Environment Variables:
        String dataBase = System.getenv("DB_DATABASE");
        String hostMySQL = System.getenv("DB_HOST");
        String urlMySQL = "jdbc:mysql://%s:3306/%s".formatted(hostMySQL, dataBase);
        String usuario = System.getenv("DB_USER");
        String senha = System.getenv("DB_PASSWORD");

//        String dataBase = System.getenv("DB_DATABASE");
//        String hostMySQL = System.getenv("DB_HOST");
//        String urlMySQL = "jdbc:mysql://%s:3306/%s".formatted(hostMySQL, dataBase);
//        String usuario = System.getenv("DB_USER");
//        String senha = System.getenv("DB_PASSWORD");

        // Instanciando o aws_connection.S3Provider
        S3Provider s3Provider = new S3Provider();
        S3Client s3Client = s3Provider.getS3Client();


        LocalDateTime dataAtual = LocalDateTime.now();
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dataFormatada = dataAtual.format(formatador); // Data e Hora Atual


        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha)) {
            logger.info("Conexão com o banco de dados estabelecida.");
            processarArquivosExcel(conexao, s3Client, dataFormatada);
            logger.info("[{}] SUCESSO - Dados inseridos com êxito!", dataFormatada);
        } catch (SQLException e) {
            logger.error("[{}] FALHA - Erro de SQL: {}", dataFormatada, e.getMessage());
        } catch (IOException e) {
            logger.error("[{}] FALHA - Erro de I/O: {}", dataFormatada, e.getMessage());
        }
    }

    private static void processarArquivosExcel(Connection conexao, S3Client s3Client, String dataFormatada) throws IOException, SQLException {
            System.out.println("Conexão com o banco de dados estabelecida.");
            System.out.println("Iniciando leitura do arquivo Excel.");

            // Baixando o arquivo do S3
            InputStream leitorExcel = S3Provider.baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);
            Workbook planilha = new XSSFWorkbook(leitorExcel);
            Sheet tabela = planilha.getSheetAt(0);

            // Continua da última linha inserida
            int ultimaLinhaLida = Counter.lerContador();

            System.out.println("INFO - Iniciando a inserção de dados...");
            System.out.println("Aguarde, Isso pode durar alguns segundos.");

            for (int i = ultimaLinhaLida + 1; i < ultimaLinhaLida + 1 + LINHAS_POR_EXECUCAO; i++) {
                if (i > tabela.getLastRowNum()) {
                    System.out.println("Não há mais linhas para processar.");
                    break;
                }
                Row linha = tabela.getRow(i);
                System.out.println("INFO - Inserindo linha " + (i + 1) + " do arquivo Excel ao Banco de Dados");

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
                        String tabelaFilial = "INSERT INTO Filial (nome, cidade, UF, submercado, fkMatriz) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement executarComandoFilial = conexao.prepareStatement(tabelaFilial, Statement.RETURN_GENERATED_KEYS)) {
                            executarComandoFilial.setString(1, nomeFilial);
                            executarComandoFilial.setString(2, cidade);
                            executarComandoFilial.setString(3, uf);
                            executarComandoFilial.setString(4, submercado);
                            executarComandoFilial.setInt(5, idMatriz);
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
                    executarComandoConsumo.setString(1, mes);
                    executarComandoConsumo.setDouble(2, consumo);
                    executarComandoConsumo.setDouble(3, emissaoCO2);
                    executarComandoConsumo.setInt(4, qtdArvores);
                    executarComandoConsumo.setInt(5, idFilial);
                    executarComandoConsumo.executeUpdate();
                }
            }
            Counter.atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
            planilha.close();
            System.out.println("[" + dataFormatada + "] SUCESSO - Dados inseridos com êxito!");
    }

}