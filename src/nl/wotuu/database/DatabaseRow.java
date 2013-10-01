package nl.wotuu.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteMisuseException;
import java.io.Serializable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.wotuu.database.annotations.DatabaseExclude;
import nl.wotuu.database.annotations.DatabasePrimaryKey;
import nl.wotuu.database.exceptions.DatabaseLoadException;
import proguard.annotation.KeepPublicClassMemberNames;

/**
 * Created by Wouter on 6/16/13.
 */
@KeepPublicClassMemberNames
public abstract class DatabaseRow implements ISaveable, Serializable {

    @DatabaseExclude
    public static Boolean NO_QUERIES = false;

    @DatabaseExclude
    private String tableName;

    @DatabasePrimaryKey
    public int id;

    public DatabaseRow(String tableName) {
        if (tableName == null)
            throw new NullPointerException("Table name cannot be null!");
        this.tableName = tableName;
    }

    public DatabaseRow(String tableName, int id) {
        this.id = id;
        if (tableName == null)
            throw new NullPointerException("Table name cannot be null!");
        this.tableName = tableName;
    }

    /**
     * @inheritDoc
     */
    public void onInsert(Boolean mayQueue) {
        DatabaseOpenHelper helper = DatabaseOpenHelper.getInstance();

        List<String> fieldNames = this.getDatabaseFieldNames();
        List<String> fieldValues = this.getDatabaseFieldValues();

        if (fieldNames.size() != fieldValues.size()) {
            throw new ArrayIndexOutOfBoundsException("Field names count does not equal field values count!");
        }

        if (fieldNames.size() == 0 || fieldValues.size() == 0)
            throw new SQLiteException("Cannot insert into database if implementing class doesn't have any fields!");

        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < fieldNames.size(); i++) {
            contentValues.put(fieldNames.get(i), fieldValues.get(i));
        }

        if (!DatabaseRow.NO_QUERIES) {
            helper.synchronizedInsert(this.tableName, contentValues, this, mayQueue);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onInsert() {
        this.onInsert(false);
    }

    /**
     * @inheritDoc
     */
    public void onUpdate(Boolean mayQueue) {
        if (this.id <= 0)
            throw new SQLiteException("Cannot update user whose ID is not set!");

        DatabaseOpenHelper helper = DatabaseOpenHelper.getInstance();

        List<String> fieldNames = this.getDatabaseFieldNames();
        List<String> fieldValues = this.getDatabaseFieldValues();

        if (fieldNames.size() == 0 || fieldValues.size() == 0)
            throw new SQLiteException("Cannot update database if implementing class doesn't have any fields!");

        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < fieldNames.size(); i++) {
            contentValues.put(fieldNames.get(i), fieldValues.get(i));
        }

        String whereClause = "`id` = '" + this.id + "';";

        if (!DatabaseRow.NO_QUERIES) {
            helper.synchronizedUpdate(this.tableName, contentValues, whereClause, mayQueue);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onUpdate() {
        this.onUpdate(false);
    }

    /**
     * @inheritDoc
     */
    public void onDelete(Boolean mayQueue) {
        if (this.id <= 0)
            throw new SQLiteMisuseException("Cannot delete user whose ID is not set!");

        DatabaseOpenHelper helper = DatabaseOpenHelper.getInstance();
        String query = "DELETE FROM `" + this.tableName + "`" +
                "WHERE `id` = '" + this.id + "'";

        if (!DatabaseRow.NO_QUERIES) {
            helper.synchronizedDelete(query, mayQueue);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDelete() {
        this.onDelete(false);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Boolean inDatabase() {
        return this.inDatabase(this.getDatabaseFieldNames());
    }

    /**
     * @inheritDoc
     */
    public Boolean inDatabase(List<String> compareColumns) {
        DatabaseOpenHelper helper = DatabaseOpenHelper.getInstance();
        SQLiteDatabase readableDatabase = helper.readableDatabase;
        String query = "SELECT `id` FROM `" + this.tableName + "` WHERE ";

        if (this.id < 1) {
            List<String> fieldValues = this.getDatabaseFieldValues(compareColumns);

            List<String> queryAddition = new ArrayList<String>();
            for (int i = 0; i < compareColumns.size(); i++) {
                queryAddition.add("`" + compareColumns.get(i) + "` = " + DatabaseUtils.sqlEscapeString(fieldValues.get(i)));
            }

            query += Utils.join(queryAddition, " AND ");
        } else
            query += "`id` = '" + this.id + "'";

        if (!DatabaseRow.NO_QUERIES) {
            // synchronized (readableDatabase){
            Cursor cursor = readableDatabase.rawQuery(query, null);
            try {
                if (cursor.moveToFirst()) {
                    this.id = cursor.getInt(0);
                    cursor.close();
                    return true;
                }
            } finally {
                // Close the cursor
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Checks if this row is in the database already or not.
     *
     * @param compareColumns The strings corresponding to the columns you'd like to load.
     * @return If the row is in the database already or not.
     */
    public Boolean inDatabase(String... compareColumns) {
        return this.inDatabase(new ArrayList<String>(Arrays.asList(compareColumns)));
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onLoad() {
        if (this.id <= 0) {
            throw new IllegalArgumentException("Cannot load object from database whose ID is not set.");
        }

        DatabaseOpenHelper helper = DatabaseOpenHelper.getInstance();
        SQLiteDatabase readableDatabase = helper.readableDatabase;
        String query = "SELECT * FROM `" + this.tableName + "` WHERE `id` = '" + this.id + "'";

        if (!DatabaseRow.NO_QUERIES) {
            Cursor cursor = readableDatabase.rawQuery(query, null);
            try {
                Field[] fields = this.getDatabaseFields();
                if (cursor.moveToFirst()) {
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String columnName = cursor.getColumnName(i);
                        for (Field field : fields) {
                            if (field.getName().equals(columnName)) {
                                try {
                                    field.set(this, this.getValueOfCursor(cursor, field.getType(), i));
                                    break;
                                } catch (IllegalAccessException e) {
                                    throw new NullPointerException("Cannot fetch value from database! Type of column name " +
                                            columnName + " is not supported.");
                                }
                            }
                        }
                    }
                } else Logger.e("Cannot find database row with id = '" + this.id + "'!");
            } finally {
                // Close the cursor
                cursor.close();
            }
        }
    }

    /**
     * Attempts to load this database row based on the passed column names.
     *
     * @param columnNames The column names to match this row with.
     */
    public void onLoad(List<String> columnNames) {
        if (columnNames.size() <= 0) {
            throw new IllegalArgumentException("Unable to load data from class '" + this.getClass().getName() +
                    "' when 0 column names are given");
        }

        // Check if this user's ID is set
        SQLiteDatabase readableDatabase = DatabaseOpenHelper.getInstance().readableDatabase;
        String query = "SELECT `id` " +
                "FROM `" + DatabaseOpenHelper.getInstance().getTableName(this.getClass()) + "`";

        List<String> values = this.getDatabaseFieldValues(columnNames);

        List<String> queryAddition = new ArrayList<String>();
        int count = 0;
        for (String value : values) {
            queryAddition.add(" `" + columnNames.get(count) + "` = " + DatabaseUtils.sqlEscapeString(value));
            count++;
        }

        if (queryAddition.size() == 0) {
            throw new DatabaseLoadException(this, "Unable to load data from class '" + this.getClass().getName() +
                    "' when query would not contain WHERE clause.");
        } else {
            query += " WHERE " + Utils.join(queryAddition, " AND ");
        }

        Cursor cursor = readableDatabase.rawQuery(query, null);
        try {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                // Fetch the ID
                this.id = cursor.getInt(0);
                // load by id now
                this.onLoad();
            } else {
                // Close the cursor
                cursor.close();
                if (cursor.getCount() == 0)
                    throw new DatabaseLoadException(this, "Didn't find a matching row to load this DatabaseRow from (query: '" + query + "')");
                else if (cursor.getCount() > 1)
                    throw new DatabaseLoadException(this, "Found multiple rows matching the query ('" + query + "') (" + cursor.getCount() + ")");
            }
        } finally {
            // Close the cursor
            cursor.close();
        }
    }

    /**
     * Attempts to load this database row based on the passed column names.
     *
     * @param compareColumns The column names to match this row with.
     */
    public void onLoad(String... compareColumns) {
        this.onLoad(new ArrayList<String>(Arrays.asList(compareColumns)));
    }

    /**
     * Get the data of the cursor on a certain build_order_index, based on the type.
     *
     * @param cursor The cursor that contains the data.
     * @param type   The type your field is in.
     * @param index  The build_order_index of the location of the data in the cursor.
     * @return The resulting object matching to the type.
     */
    private Object getValueOfCursor(Cursor cursor, Class type, int index) {
        if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) {
            return cursor.getInt(index);
        } else if (type.isAssignableFrom(Long.class) || type.isAssignableFrom(long.class)) {
            return cursor.getLong(index);
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
            // 0 is false, otherwise is true
            return cursor.getInt(index) > 0;
            /*
            String value = cursor.getString(index);
            if (value == null) return false;
            return value.toLowerCase().equals("true");
            */
        } else if (type.isAssignableFrom(String.class)) {
            return cursor.getString(index);
        } else if (type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) {
            return cursor.getDouble(index);
        } else if (type.isAssignableFrom(Double.class) || type.isAssignableFrom(double.class)) {
            return cursor.getFloat(index);
        } else {
            throw new IllegalArgumentException("Unable to assign value from cursor to class " + type.getName() +
                    " with index " + index + ". This type is not supported.");
        }
    }

    /**
     * Get all database fields
     *
     * @return
     */
    private Field[] getDatabaseFields() {
        List<Field> databaseFields = new ArrayList<Field>();

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!f.isAnnotationPresent(DatabaseExclude.class))
                databaseFields.add(f);
        }

        return databaseFields.toArray(new Field[0]);
    }

    /**
     * Get all field names of the implementing class, that are not decorated with the
     * DatabaseExclude annotation.
     *
     * @return The list containing the field names.
     */
    private List<String> getDatabaseFieldNames() {
        List<String> fieldNames = new ArrayList<String>();

        Field[] fields = this.getDatabaseFields();
        for (Field f : fields) {
            fieldNames.add(f.getName());
        }

        return fieldNames;
    }


    /**
     * Get all field values of the implementing class.
     *
     * @return The list containing the field values.
     */
    private List<String> getDatabaseFieldValues() {
        return this.getDatabaseFieldValues(this.getDatabaseFieldNames());
    }


    /**
     * Get all field values of the implementing class.
     *
     * @param fieldNames The list containing the field names of the values you'd like to get.
     * @return The list containing the field values.
     */
    private List<String> getDatabaseFieldValues(List<String> fieldNames) {
        if (fieldNames.size() == 0)
            throw new IllegalArgumentException("Cannot get field values of 0 fields!");

        List<String> fieldValues = new ArrayList<String>();

        Field[] fields = this.getDatabaseFields();
        // For every value we want
        for (String s : fieldNames) {
            // For every value we have
            for (Field f : fields) {
                // If there is no annotation present, and if the value we have is a value we want
                if (!f.isAnnotationPresent(DatabaseExclude.class) && fieldNames.contains(f.getName())) {
                    // If the value we want is the value we have
                    if (s.equals(f.getName())) {
                        try {
                            Object value = f.get(this);
                            if( value != null && (f.getType() == boolean.class || f.getType() == Boolean.class) ){
                                value = ((Boolean) value ? 1 : 0);
                            }
                            // Add it to the list!
                            fieldValues.add(String.valueOf(value));
                        } catch (IllegalAccessException e) {

                        }
                    }
                }
            }
        }
        return fieldValues;
    }
}
