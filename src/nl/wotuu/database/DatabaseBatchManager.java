package nl.wotuu.database;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Wouter on 7/3/13.
 */
public class DatabaseBatchManager {

    private static int PAUSE_MS = 30000;

    private DatabaseOpenHelper helper;

    private final HashMap<String, List<InsertQuery>> insertQueries;
    private final List<InsertRawQuery> insertRawQueries;
    private final List<UpdateQuery> updateQueries;
    private final List<UpdateRawQuery> updateRawQueries;
    private final List<DeleteQuery> deleteQueries;

    private Timer batchProcessorTimer;

    public DatabaseBatchManager(DatabaseOpenHelper helper) {
        this.helper = helper;

        this.insertQueries = new HashMap<String, List<InsertQuery>>();
        this.insertRawQueries = new ArrayList<InsertRawQuery>();
        this.updateQueries = new ArrayList<UpdateQuery>();
        this.updateRawQueries = new ArrayList<UpdateRawQuery>();
        this.deleteQueries = new ArrayList<DeleteQuery>();

        this.batchProcessorTimer = new Timer("BatchProcessorTimer");
        // Once every 30 seconds.
        this.batchProcessorTimer.schedule(new BatchProcessor(), PAUSE_MS, PAUSE_MS);
    }

    /**
     * Queue an insert query to be inserted at a later point in time.
     *
     * @param tableName     The table name you'd wish to insert.
     * @param contentValues The content that you wish to insert.
     * @param databaseRow   The database row where there inserted ID must be assigned to.
     */
    public void queueInsert(String tableName, ContentValues contentValues, DatabaseRow databaseRow) {
        if (!this.insertQueries.containsKey(tableName)) {
            this.insertQueries.put(tableName, new ArrayList<InsertQuery>());
        }
        this.insertQueries.get(tableName).add(new InsertQuery(tableName, contentValues, databaseRow));
    }

    /**
     * Queue a raw insert query to be inserted at a later point in time.
     *
     * @param rawQuery The raw query to execute (must contain INSERT!)
     */
    public void queueInsert(String rawQuery) {
        if (!rawQuery.toLowerCase().contains("insert"))
            throw new IllegalArgumentException("Passed raw query was not an insert query!");

        this.insertRawQueries.add(new InsertRawQuery(rawQuery));
    }

    /**
     * Queue an update query to be inserted at a later point in time.
     *
     * @param tableName     The table name you wish to alter.
     * @param contentValues The content's values you wish to edit
     * @param whereClause   The where clause.
     */
    public void queueUpdate(String tableName, ContentValues contentValues, String whereClause) {
        this.updateQueries.add(new UpdateQuery(tableName, contentValues, whereClause));
    }

    /**
     * Queue an update query to be executed at a later point in time.
     *
     * @param rawQuery The raw query to execute (must contain UPDATE!)
     */
    public void queueUpdate(String rawQuery) {
        if (!rawQuery.toLowerCase().contains("update"))
            throw new IllegalArgumentException("Passed raw query was not an update query!");

        this.updateRawQueries.add(new UpdateRawQuery(rawQuery));
    }

    /**
     * Queue a delete query to be executed at a later point in time.
     *
     * @param rawQuery The raw query to execute (must contain DELETE!)
     */
    public void queueDelete(String rawQuery) {
        if (!rawQuery.toLowerCase().contains("delete"))
            throw new IllegalArgumentException("Passed raw query was not a delete query!");

        this.deleteQueries.add(new DeleteQuery(rawQuery));
    }


    public class InsertQuery {
        public String tableName;
        public ContentValues contentValues;
        public DatabaseRow databaseRow;

        public InsertQuery(String tableName, ContentValues contentValues, DatabaseRow databaseRow) {
            this.tableName = tableName;
            this.contentValues = contentValues;
            this.databaseRow = databaseRow;
        }
    }

    public class InsertRawQuery {
        public String rawQuery;

        public InsertRawQuery(String rawQuery) {
            this.rawQuery = rawQuery;
        }
    }

    public class UpdateQuery {
        public String tableName;
        public ContentValues contentValues;
        public String whereClause;

        public UpdateQuery(String tableName, ContentValues contentValues, String whereClause) {
            this.tableName = tableName;
            this.contentValues = contentValues;
            this.whereClause = whereClause;
        }
    }

    public class UpdateRawQuery {
        public String rawQuery;

        public UpdateRawQuery(String rawQuery) {
            this.rawQuery = rawQuery;
        }
    }

    public class DeleteQuery {
        public String rawQuery;

        public DeleteQuery(String rawQuery) {
            this.rawQuery = rawQuery;
        }
    }

    public class BatchProcessor extends TimerTask {
        /**
         * Processes all insert queries. Uses InsertHelpers to speed up the process.
         */
        public void processInsertQueries() {
            // Logger.d("Batch processing insert queries start ..");
            int queryCount = 0;
            Stopwatch stopwatch = new Stopwatch();

            SQLiteDatabase writeableDatabase = helper.writeableDatabase;
            writeableDatabase.beginTransaction();
            try {
                synchronized (insertQueries) {
                    // Batch insert the insert statements
                    for (Map.Entry<String, List<InsertQuery>> entry : insertQueries.entrySet()) {
                        DatabaseUtils.InsertHelper insertHelper = helper.getInsertHelper(entry.getKey());

                        for (InsertQuery query : entry.getValue()) {
                            query.databaseRow.id = (int) insertHelper.insert(query.contentValues);
                            queryCount++;
                        }
                    }

                    insertQueries.clear();
                }
                synchronized (insertRawQueries) {
                    for (InsertRawQuery query : insertRawQueries) {
                        writeableDatabase.execSQL(query.rawQuery);
                        queryCount++;
                    }

                    insertRawQueries.clear();
                }
                writeableDatabase.setTransactionSuccessful();
            } finally {
                writeableDatabase.endTransaction();
            }
            // Logger.d("Batch processing insert queries finish! Processed " + queryCount + " inserts in " + stopwatch.elapsedTimeSeconds() + " s.");
        }

        /**
         * Processes all update queries at once.
         */
        public void processUpdateQueries() {
            // Logger.d("Batch processing update queries start ..");
            int queryCount = 0;
            Stopwatch stopwatch = new Stopwatch();
            SQLiteDatabase writeableDatabase = helper.writeableDatabase;
            writeableDatabase.beginTransaction();
            try {
                synchronized (updateQueries) {
                    for (UpdateQuery query : updateQueries) {
                        writeableDatabase.update(query.tableName, query.contentValues, query.whereClause, null);
                        queryCount++;
                    }

                    updateQueries.clear();
                }
                synchronized (updateRawQueries) {
                    for (UpdateRawQuery query : updateRawQueries) {
                        writeableDatabase.execSQL(query.rawQuery);
                        queryCount++;
                    }

                    updateRawQueries.clear();
                }

                writeableDatabase.setTransactionSuccessful();
            } finally {
                writeableDatabase.endTransaction();
            }
            // Logger.d("Batch processing update queries finish! Processed " + queryCount + " updates in " + stopwatch.elapsedTimeSeconds() + " s.");
        }

        /**
         * Processes all delete queries at once.
         */
        public void processDeleteQueries() {
            SQLiteDatabase writeableDatabase = helper.writeableDatabase;
            writeableDatabase.beginTransaction();
            try {
                synchronized (deleteQueries) {
                    for (DeleteQuery query : deleteQueries) {
                        writeableDatabase.execSQL(query.rawQuery);
                    }

                    deleteQueries.clear();
                }
                writeableDatabase.setTransactionSuccessful();
            } finally {
                writeableDatabase.endTransaction();
            }
        }

        @Override
        public void run() {
            this.processInsertQueries();

            this.processUpdateQueries();

            this.processDeleteQueries();
        }
    }
}
