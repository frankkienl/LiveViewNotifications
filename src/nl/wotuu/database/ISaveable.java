package nl.wotuu.database;

import java.util.List;

/**
 * Created by Wouter on 15/06/13.
 */
public interface ISaveable {

    /**
     * Insert this class into the database.
     */
    void onInsert();

    /**
     * Update this class in the database.
     */
    void onUpdate();

    /**
     * Return a value representing if this instance is in the database or not.
     * @return Yes if this instance exists in the database, no if not.
     */
    Boolean inDatabase();

    /**
     * Return a value representing if this instance is in the database or not.
     * @param compareColumns The columns to use to compare with other rows in the database.
     * @return Yes if this instance exists in the database, no if not.
     */
    Boolean inDatabase(List<String> compareColumns);

    /**
     * Delete this class from the database.
     */
    void onDelete();

    /**
     * load this class from the database.
     */
    void onLoad();
}
