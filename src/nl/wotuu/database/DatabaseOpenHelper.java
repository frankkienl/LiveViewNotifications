package nl.wotuu.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.frankkie.livenotifications.databaserows.IgnoreItem;

import nl.wotuu.database.annotations.DatabaseExclude;
import nl.wotuu.database.annotations.DatabasePrimaryKey;

/**
 * Created by Wouter on 6/11/13.
 */
public class DatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 32;

    private static DatabaseOpenHelper instance;


    private final Object databaseSyncLock = new Object();

    /**
     * Get the writeable database of the open helper.
     */
    public SQLiteDatabase writeableDatabase;

    /**
     * Get the readable database of the open helper.
     */
    public SQLiteDatabase readableDatabase;

    /**
     * Hashmap containing all table names
     */
    private List<TableNameMap> tableNames;

    /**
     * Tablename to insert helper mapping.
     */
    private HashMap<String, DatabaseUtils.InsertHelper> insertHelpers;

    /**
     * The batch manager for insert queries.
     */
    private DatabaseBatchManager batchManager;

    /**
     * The context used to create the open helper.
     */
    private Context context;

    /**
     * Creates a new database helper. Can only be called once for effect. Multiple times will just return
     * the first instance.
     *
     * @param context The activity to create the instance from.
     * @return The DatabaseOpenHelper
     */
    public static DatabaseOpenHelper createInstance(Context context) {
        if (instance == null)
            instance = new DatabaseOpenHelper(context);
        return instance;
    }

    /**
     * Gets the instance of the DatabaseOpenHelper.
     *
     * @return The DatabaseOpenHelper instance.
     */
    public static DatabaseOpenHelper getInstance() {
        if (instance == null)
            throw new NullPointerException("Call createInstance first!");
        return instance;
    }


    DatabaseOpenHelper(Context context) {
        super(context, "frankkienl_livenotifications", null, DATABASE_VERSION);
        this.context = context;

        this.tableNames = new ArrayList<TableNameMap>();
        this.tableNames.add(new TableNameMap("ignoreitem", IgnoreItem.class));
       
        this.writeableDatabase = this.getWritableDatabase();
        this.readableDatabase = this.getReadableDatabase();

        this.batchManager = new DatabaseBatchManager(this);
    }

    /**
     * Get the insert helper for a certain table.
     *
     * @param tableName The name of the table you'd like to get the insert helper from.
     * @return The inserthelper, or null if none exists.
     */
    public DatabaseUtils.InsertHelper getInsertHelper(String tableName) {
        for (Map.Entry<String, DatabaseUtils.InsertHelper> entry : this.insertHelpers.entrySet()) {
            if (entry.getKey().equals(tableName))
                return entry.getValue();
        }
        return null;
    }

    /**
     * Get the table name corresponding to a certain class.
     *
     * @param c The Class definition you'd like to get the table name for.
     * @return The tablename, or null otherwise!
     */
    public String getTableName(Class c) {
        for (TableNameMap entry : this.tableNames) {
            if (entry.tableClass == c)
                return entry.tableName;
        }
        return null;
    }

    @Override
    public void onOpen(SQLiteDatabase sqLiteDatabase) {
        super.onOpen(sqLiteDatabase);

        this.insertHelpers = new HashMap<String, DatabaseUtils.InsertHelper>();
        for (TableNameMap tableNameMap : this.tableNames) {
            this.insertHelpers.put(tableNameMap.tableName, new DatabaseUtils.InsertHelper(sqLiteDatabase, tableNameMap.tableName));
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Drop the table as it's subject to change while testing!
        for (TableNameMap tableNameMap : this.tableNames) {
            db.execSQL(this.createTable(tableNameMap.tableClass, tableNameMap.tableName));
        }               
    }

    public void makeDefaultItems(){
        /* MOD BY @F */
        //Add default rows
        IgnoreItem i1 = new IgnoreItem();
        i1.packageName = "android";
        i1.description = "ignore Toast";
        i1.appName = "android";
        i1.onInsert();
        IgnoreItem i2 = new IgnoreItem();
        i2.packageName = "android";
        i2.description = "trust me on this one";
        i2.appName = "System-UI";
        i2.onInsert();
        IgnoreItem i3 = new IgnoreItem();
        i3.packageName = "android";
        i3.description = "vertrouw me, dit is nodig";
        i3.appName = "Systeem-UI";
        i3.onInsert();
        IgnoreItem i4 = new IgnoreItem();
        i4.packageName = "android";
        i4.description = "vertrouw me, dit is nodig";
        i4.appName = "Google Zoeken";
        i4.onInsert();
        IgnoreItem i5 = new IgnoreItem();
        i5.packageName = "android";
        i5.description = "trust me on this one";
        i5.appName = "Google Search";
        i5.onInsert();
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        //not needed yet
    }

    /**
     * Performs a synchronized insert of an insert query.
     *
     * @param tableName     The table name that is inserted in.
     * @param contentValues The inserted values.
     * @param databaseRow   The database row that will receive the inserted ID.
     * @param mayQueue      If the insert is allowed to be queued (the DatabaseRow will receive the ID of the inserted row)
     */
    public void synchronizedInsert(String tableName, ContentValues contentValues, DatabaseRow databaseRow, Boolean mayQueue) {
        if (mayQueue) {
            this.batchManager.queueInsert(tableName, contentValues, databaseRow);
        } else {
            synchronized (databaseSyncLock) {
                databaseRow.id = (int) this.writeableDatabase.insert(tableName, null, contentValues);
            }
        }
    }

    /**
     * Performs a synchronized insert of an insert query.
     *
     * @param rawQuery The raw insert query.
     * @param mayQueue If the query may be queued or not (does not set the received id!).
     */
    public void synchronizedInsert(String rawQuery, Boolean mayQueue) {
        if (mayQueue) {
            this.batchManager.queueInsert(rawQuery);
        } else {
            synchronized (databaseSyncLock) {
                this.writeableDatabase.execSQL(rawQuery);
            }
        }
    }

    /**
     * Performs a synchronized update of an update query.
     *
     * @param tableName     The table name that is updateed in.
     * @param contentValues The updateed values.
     * @param whereClause   The where clause which matches all rows that must be updated.
     * @param mayQueue      If the update is allowed to be queued.
     */
    public void synchronizedUpdate(String tableName, ContentValues contentValues, String whereClause, Boolean mayQueue) {
        if (mayQueue) {
            this.batchManager.queueUpdate(tableName, contentValues, whereClause);
        } else {
            synchronized (databaseSyncLock) {
                this.writeableDatabase.update(tableName, contentValues, whereClause, null);
            }
        }
    }

    /**
     * Performs a synchronized update of an update query.
     *
     * @param rawQuery The raw update query.
     * @param mayQueue If the query may be queued or not.
     */
    public void synchronizedUpdate(String rawQuery, Boolean mayQueue) {
        if (mayQueue) {
            this.batchManager.queueUpdate(rawQuery);
        } else {
            synchronized (databaseSyncLock) {
                this.writeableDatabase.execSQL(rawQuery);
            }
        }
    }

    /**
     * Performs a synchronized delete of an delete query.
     *
     * @param rawQuery The raw delete query.
     * @param mayQueue If the query may be queued or not.
     */
    public void synchronizedDelete(String rawQuery, Boolean mayQueue) {
        if (mayQueue) {
            this.batchManager.queueDelete(rawQuery);
        } else {
            synchronized (databaseSyncLock) {
                this.writeableDatabase.execSQL(rawQuery);
            }
        }
    }

    /**
     * Makes a create table query from a class definition.
     *
     * @param c         The class definition you'd like to create a CREATE TABLE from.
     * @param tableName The name of the table you'd like to have created.
     * @return The string containing the CREATE TABLE query;
     */
    public String createTable(Class c, String tableName) {
        Field[] fields = c.getFields();
        if (fields.length == 0) {
            throw new IllegalArgumentException("Cannot create Create Table SQL string from class without fields.");
        }

        String result = "CREATE TABLE IF NOT EXISTS `" + tableName + "` ";

        Field primaryKeyField = null;

        List<String> fieldStrings = new ArrayList<String>();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(DatabaseExclude.class)) {
                String fieldString = "`" + field.getName() + "` ";


                if (field.isAnnotationPresent(DatabasePrimaryKey.class)) {
                    if (primaryKeyField != null) {
                        throw new UnsupportedOperationException("Cannot have database table with two primary keys.");
                    }
                    if (!field.getType().isAssignableFrom(Integer.class) && !field.getType().isAssignableFrom(int.class)) {
                        throw new UnsupportedOperationException("Cannot have primary key that is not of Integer or int type!.");
                    }
                    primaryKeyField = field;
                    fieldString += " INTEGER PRIMARY KEY AUTOINCREMENT";
                } else {

                    if (field.getType().isAssignableFrom(Integer.class) || field.getType().isAssignableFrom(int.class) ||
                            field.getType().isAssignableFrom(Boolean.class) || field.getType().isAssignableFrom(boolean.class) ||
                            field.getType().isAssignableFrom(Long.class) || field.getType().isAssignableFrom(long.class)) {
                        fieldString += " INT ";
                    } else if (field.getType().isAssignableFrom(String.class)) {
                        fieldString += " TEXT ";
                    } else if (field.getType().isAssignableFrom(Float.class) || field.getType().isAssignableFrom(float.class) ||
                            field.getType().isAssignableFrom(Double.class) || field.getType().isAssignableFrom(double.class)) {
                        fieldString += " REAL ";
                    } else fieldString += " NOT NULL";
                }

                fieldStrings.add(fieldString);
            }
        }

        if (primaryKeyField == null) {
            throw new UnsupportedOperationException("Cannot have database table without a primary key! (For your own good.)");
        }

        result += " (" + Utils.join(fieldStrings, ", ") + ") ";

        return result;
    }

    private class TableNameMap {
        public String tableName;
        public Class tableClass;

        private TableNameMap(String tableName, Class tableClass) {
            this.tableName = tableName;
            this.tableClass = tableClass;
        }
    }
}
