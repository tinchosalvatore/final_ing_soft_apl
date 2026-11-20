package com.trackergastos.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

// PRUEBA DESCARTABLE (spec 005-CA04): SQL armado concatenando texto del usuario. No se mergea.
@Service
public class BusquedaInseguraService {

    private final DataSource dataSource;

    public BusquedaInseguraService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int contarPorDescripcion(String descripcion) throws SQLException {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("select count(*) from gasto where descripcion = '" + descripcion + "'");
            rs.next();
            return rs.getInt(1);
        }
    }
}
