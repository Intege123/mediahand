package mediahand.repository.base;

import java.sql.SQLException;
import java.util.List;

public interface BaseRepository<T> {

    T create(T entry) throws SQLException;

    T update(T entry) throws SQLException;

    void remove(T entry) throws SQLException;

    T find(T entry) throws SQLException;

    List<T> findAll() throws SQLException;

}
