package pl.szczodrzynski.edziennik.datamodels;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface GradeCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(GradeCategory gradeCategory);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<GradeCategory> gradeCategoryList);

    @Query("DELETE FROM gradeCategories WHERE profileId = :profileId")
    void clear(int profileId);

    @Query("SELECT * FROM gradeCategories WHERE profileId = :profileId AND categoryId = :categoryId")
    GradeCategory getByIdNow(int profileId, int categoryId);

    @Query("SELECT * FROM gradeCategories WHERE profileId = :profileId")
    LiveData<List<GradeCategory>> getAll(int profileId);

    @Query("SELECT * FROM gradeCategories WHERE profileId = :profileId")
    List<GradeCategory> getAllNow(int profileId);
}

