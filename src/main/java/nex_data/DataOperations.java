package nex_data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class DataOperations {
    public void inserirDadosPrevistos(Connection conexao, LocalDate dataReferencia, double consumoPrevisto, int fkFilial) throws SQLException {
        String query = "INSERT INTO DadosPrevistos (dataReferencia, consumoPrevisto, fkFilial) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(dataReferencia));
            stmt.setDouble(2, consumoPrevisto);
            stmt.setInt(3, fkFilial);
            stmt.executeUpdate();
        }
    }

    public void inserirDadosHorario(Connection conexao, LocalDate dataReferencia, LocalTime hora, double consumoHorario, int fkFilial) throws SQLException {
        String query = "INSERT INTO DadosHorario (dataReferencia, hora, consumoHorario, fkFilial) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(dataReferencia));
            stmt.setTime(2, java.sql.Time.valueOf(hora));
            stmt.setDouble(3, consumoHorario);
            stmt.setInt(4, fkFilial);
            stmt.executeUpdate();
        }
    }

    public static double obterConsumoMesAnterior(Connection conexao, LocalDate dataReferencia, int fkFilial) throws SQLException {
        String query = "SELECT consumoEnergia FROM ConsumoDados WHERE dataReferencia = ? AND fkFilial = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(dataReferencia));
            stmt.setInt(2, fkFilial);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double consumoEnergia = rs.getDouble("consumoEnergia");
                    return Double.isNaN(consumoEnergia) ? 0.0 : consumoEnergia;
                } else {
                    return 0.0; // Valor padrão se não houver dados
                }
            }
        }
    }
}
