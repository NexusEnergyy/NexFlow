import java.io.*;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;

public class ExcelSQL {
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String CONTADOR_ARQUIVO = "contador.txt"; // Onde sera armazenado a ultima linha lida pelo for
    private static final String BUCKET_NAME = "bucketnexus"; // Nome do bucket
    private static final String FILE_KEY = "DadosConsumo.xlsx";    // Nome do arquivo no bucket

    public static void main(String[] args) {
        String urlMySQL = "jdbc:mysql://107.23.245.191:3306/nexusEnergy";
        String usuario = "root";
        String senha = "urubu100";

        // Instanciando o S3Provider
        S3Provider s3Provider = new S3Provider();
        S3Client s3Client = s3Provider.getS3Client();

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha)) {
            System.out.println("Conexão com o banco de dados estabelecida.");
            System.out.println("Iniciando leitura do arquivo Excel.");

            // Baixando o arquivo do S3
            InputStream leitorExcel = baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);
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

                // Aqui continua seu código de leitura de células e inserção no banco de dados...
            }

            atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
            planilha.close();
            System.out.println("SUCESSO - Dados inseridos com êxito!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FALHA - Houve um erro ao inserir os dados.");
        }
    }

    // Função para baixar arquivo do S3
    private static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(request);
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
