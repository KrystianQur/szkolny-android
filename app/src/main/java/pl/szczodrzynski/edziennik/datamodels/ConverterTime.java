package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.TypeConverter;

import pl.szczodrzynski.edziennik.models.Time;

public class ConverterTime {

    @TypeConverter
    public static Time toTime(String value) {
        return value == null ? null : value.equals("null") ? null : Time.fromHms(value);
    }

    @TypeConverter
    public static String toString(Time value) {
        return value == null ? null : value.getStringValue();
    }
}
