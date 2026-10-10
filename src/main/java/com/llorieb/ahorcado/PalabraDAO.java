package com.llorieb.ahorcado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PalabraDAO {
    public String seleccionarPalabraPorCategoria(String categoria) {
        String palabraSeleccionada = "";
        Connection connection = DatabaseConnection.connect();

        if (connection == null) {
            return palabraSeleccionada;
        }

        String sql = "SELECT palabra FROM palabras WHERE desc_categoria = ? ORDER BY RANDOM() LIMIT 1";

        /*
         * La conexión es compartida por la aplicación, pero statement y
         * ResultSet pertenecen a cada consulta. Se cierran inmediatamente para
         * no acumular recursos nativos de SQLite durante sesiones largas.
         */
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, categoria);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    palabraSeleccionada = resultSet.getString("palabra");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return palabraSeleccionada;
    }
}
