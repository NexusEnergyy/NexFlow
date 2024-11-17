package nex_data;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

public class DataManipulation extends DataOperations {





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
}
