package nl.frankkie.livenotifications.databaserows;

import nl.wotuu.database.DatabaseOpenHelper;
import nl.wotuu.database.DatabaseRow;
import nl.wotuu.database.annotations.DatabaseExclude;
import proguard.annotation.KeepPublicClassMemberNames;

/**
 *
 * @author FrankkieNL
 */
@KeepPublicClassMemberNames
public class IgnoreItem extends DatabaseRow {
    @DatabaseExclude
    private static final long serialVersionUID = 1L;
    public String packageName = "";
    public String appName = "";
    public String description = "";
    
  public IgnoreItem() {
        super(DatabaseOpenHelper.getInstance().getTableName(IgnoreItem.class));
    }

    public IgnoreItem(int id) {
        super(DatabaseOpenHelper.getInstance().getTableName(IgnoreItem.class), id);
    }
}
