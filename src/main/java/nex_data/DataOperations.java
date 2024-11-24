package nex_data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataOperations {
    protected double obterConsumoMensal(Connection conexao) throws SQLException {
        String query = "SELECT SUM(consumoEnergia) AS consumoMensal FROM ConsumoDados WHERE MONTH(dataReferencia) = MONTH(CURRENT_DATE())";
        try (PreparedStatement stmt = conexao.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("consumoMensal");
            }
        }
        return 0.0;
    }

    protected void inserirPrevisaoConsumoMensal(Connection conexao, double previsaoConsumoMensal) throws SQLException {
        String query = "INSERT INTO PrevisaoConsumoMensal (dataPrevisao, consumoPrevisto) VALUES (CURRENT_DATE(), ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            stmt.setDouble(1, previsaoConsumoMensal);
            stmt.executeUpdate();
        }
    }

    protected void inserirConsumoHorario(Connection conexao, double[] consumoHorario) throws SQLException {
        String query = "INSERT INTO ConsumoHorario (hora, consumo) VALUES (?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            for (int h = 0; h < 24; h++) {
                stmt.setInt(1, h);
                stmt.setDouble(2, consumoHorario[h]);
                stmt.executeUpdate();
            }
        }
    }
}
