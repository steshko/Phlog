package edu.miami.cs.steshko.phlogging.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataRoomAccess {
    @Query("SELECT * FROM Phlog")
    List<DataRoomEntity> getList();

    @Insert
    void addPhlog(DataRoomEntity newPhlog);

    @Delete
    void deletePhlog(DataRoomEntity oldPhlog);

    @Update
    void updatePhlog(DataRoomEntity updatePhlog);
}
