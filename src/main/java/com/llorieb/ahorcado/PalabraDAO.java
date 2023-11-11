package com.llorieb.ahorcado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PalabraDAO {
    public String seleccionarPalabraPorCategoria(String categoria) {
        String palabraSeleccionada = "";
        Connection connection = DatabaseConnection.connect();
        if (connection != null) {
            try {
                String sql = "SELECT palabra FROM palabras WHERE desc_categoria = ? ORDER BY RANDOM() LIMIT 1";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, categoria);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    palabraSeleccionada = resultSet.getString("palabra");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseConnection.closeConnection();
            }
        }
        return palabraSeleccionada;
    }
}
