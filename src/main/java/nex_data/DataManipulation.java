package nex_data;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DataManipulation extends DataOperations {

    public static LocalDate converterYYYYMMParaDate(String mesReferencia) {
        try {
            String dataFormatada = mesReferencia + "01";
            DateTimeFormatter formatadorEntrada = DateTimeFormatter.ofPattern("yyyyMMdd");

            return LocalDate.parse(dataFormatada, formatadorEntrada);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            System.err.println("Erro ao converter a data: " + mesReferencia + " - " + e.getMessage());
            return null;
        }
    }

    public static double getCellValueAsDouble(Cell cell) {
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

    public static String getCellValueAsString(Cell cell) {
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

    public static double[] calcularConsumoHorario(double consumoMensal) {
        double consumoDiario = consumoMensal / 30;
        double[] consumoHorario = new double[24];
        double[] horariosEspecificos = {0, 3, 6, 9, 12, 15, 18, 21};
        double variacao = consumoDiario / horariosEspecificos.length;

        for (int i = 0; i < horariosEspecificos.length; i++) {
            consumoHorario[(int) horariosEspecificos[i]] = variacao * (i + 1);
        }

        return consumoHorario;
    }

    public static double calcularPrevisaoConsumo(double consumoMesAnterior, double consumoDoisMesesAtras) {
        double taxaCrescimento = (consumoMesAnterior - consumoDoisMesesAtras) / consumoDoisMesesAtras;
        if (taxaCrescimento < 0) {
            taxaCrescimento = 0;
        }
        return consumoMesAnterior * (1 + taxaCrescimento);
    }
}