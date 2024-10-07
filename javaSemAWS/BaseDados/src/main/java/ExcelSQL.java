import java.io.*;
import java.net.URL;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelSQL {
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String CONTADOR_ARQUIVO = "contador.txt"; // Onde sera armazenado a ultima linha lida pelo for

    public static void main(String[] args) {
        String caminhoArquivo = "https://nexusenergybucket.s3.amazonaws.com/DadosConsumo.xlsx";
        String urlMySQL = "jdbc:mysql://localhost:3306/nexusEnergy";
        String usuario = "root";
        String senha = "Vidanova-123";

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha)) {
            System.out.println("Conexão com o banco de dados estabelecida.");
            System.out.println("Iniciando leitura do arquivo Excel.");
            InputStream leitorExcel = new URL(caminhoArquivo).openStream();
            Workbook planilha = new XSSFWorkbook(leitorExcel);
            Sheet tabela = planilha.getSheetAt(0);

            int ultimaLinhaLida = lerContador();

            System.out.println("INFO - Iniciando a inserção de dados...");
            System.out.println("Isso pode durar alguns segundos.");

            for (int i = ultimaLinhaLida + 1; i < ultimaLinhaLida + 1 + LINHAS_POR_EXECUCAO; i++) {
                if (i > tabela.getLastRowNum()) {
                    System.out.println("Não há mais linhas para processar.");
                    break;
                }
                Row linha = tabela.getRow(i);
                System.out.println("Lendo e inserindo linha " + (i + 1) + " do arquivo Excel.");

                // Lê os valores das células
                String nome = getCellValueAsString(linha.getCell(1));
                String cnpj = getCellValueAsString(linha.getCell(3));
                Double ativoTotal = getCellValueAsDouble(linha.getCell(8));

                String nomeFilial = getCellValueAsString(linha.getCell(2));
                String cidade = getCellValueAsString(linha.getCell(4));
                String uf = getCellValueAsString(linha.getCell(5));
                String submercado = getCellValueAsString(linha.getCell(6));

                String mes = getCellValueAsString(linha.getCell(0));
                Double consumo = getCellValueAsDouble(linha.getCell(7));
                Double emissaoCO2 = consumo * 81;
                Integer qtdArvores = (int) Math.round(emissaoCO2 / 200);

                // Verifica se o CNPJ já existe na tabela Matriz
                String queryVerificaMatriz = "SELECT idMatriz FROM Matriz WHERE CNPJ = ?";
                int idMatriz = 0;
                try (PreparedStatement verificarMatriz = conexao.prepareStatement(queryVerificaMatriz)) {
                    verificarMatriz.setString(1, cnpj);
                    ResultSet rsMatriz = verificarMatriz.executeQuery();
                    if (rsMatriz.next()) {
                        idMatriz = rsMatriz.getInt("idMatriz");  // Já existe, obtém o idMatriz
                        System.out.println("CNPJ já existe na tabela Matriz, idMatriz: " + idMatriz);
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
                // Verifica se o nome da Filial e a cidade já existem na tabela Filial
                String queryVerificaFilial = "SELECT idFilial FROM Filial WHERE nome = ? AND cidade = ?";
                int idFilial = 0;
                try (PreparedStatement verificarFilial = conexao.prepareStatement(queryVerificaFilial)) {
                    verificarFilial.setString(1, nomeFilial);
                    verificarFilial.setString(2, cidade);
                    ResultSet rsFilial = verificarFilial.executeQuery();
                    if (rsFilial.next()) {
                        idFilial = rsFilial.getInt("idFilial");  // Já existe, obtém o idFilial
                        System.out.println("Filial com nome '" + nomeFilial + "' e cidade '" + cidade + "' já existe, idFilial: " + idFilial);
                    } else {
                        // Insere na tabela Filial, caso não exista
                        String tabelaFilial = "INSERT INTO Filial (nome, cidade, uf, submercado, fkMatriz) VALUES (?, ?, ?, ?, ?)";
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

            atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
            planilha.close();
            System.out.println("SUCESSO - Dados inseridos com êxito!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FALHA - Houve um erro ao inserir os dados.");
        }
    }

    // Método para reiniciar o contador
    private static void reiniciarContador() {
        atualizarContador(0); // Define o contador para 0
    }

    // Método para ler o contador do arquivo
    private static int lerContador() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONTADOR_ARQUIVO))) {
            String linha = br.readLine();
            return linha != null ? Integer.parseInt(linha) : 0; // Retorna 0 se o arquivo estiver vazio
        } catch (IOException e) {
            return 0; // Retorna 0 se houver erro ao ler o arquivo
        }
    }

    // Método para atualizar o contador no arquivo
    private static void atualizarContador(int novaLinha) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONTADOR_ARQUIVO))) {
            bw.write(String.valueOf(novaLinha));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return 0.0;  // Retorna 0.0 se a célula estiver vazia
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();  // Retorna o valor como Double
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());  // Converte String para Double
                } catch (NumberFormatException e) {
                    return 0.0;  // Retorna 0.0 se a conversão falhar
                }
            default:
                return 0.0;
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
