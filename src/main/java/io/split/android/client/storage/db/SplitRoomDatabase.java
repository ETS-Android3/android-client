package io.split.android.client.storage.db;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;

@Database(
        entities = {
                MySegmentEntity.class, SplitEntity.class, EventEntity.class,
                ImpressionEntity.class, GeneralInfoEntity.class, ImpressionsCountEntity.class,
                AttributesEntity.class
        },
        version = 3
)
public abstract class SplitRoomDatabase extends RoomDatabase {

    public abstract MySegmentDao mySegmentDao();

    public abstract SplitDao splitDao();

    public abstract EventDao eventDao();

    public abstract ImpressionDao impressionDao();

    public abstract GeneralInfoDao generalInfoDao();

    public abstract ImpressionsCountDao impressionsCountDao();

    public abstract AttributesDao attributesDao();

    private volatile SplitQueryDao mSplitQueryDao;

    private static volatile Map<String, SplitRoomDatabase> mInstances = new ConcurrentHashMap<>();

    public static SplitRoomDatabase getDatabase(final Context context, final String databaseName) {
        checkNotNull(context);
        checkNotNull(databaseName);
        checkArgument(!databaseName.isEmpty());
        SplitRoomDatabase instance = null;
        synchronized (SplitRoomDatabase.class) {
            instance = mInstances.get(databaseName);
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                        SplitRoomDatabase.class, databaseName)
                        .fallbackToDestructiveMigrationFrom(1, 2)
                        .build();
                mInstances.put(databaseName, instance);
            }
        }
        return instance;
    }

    public SplitQueryDao splitQueryDao() {
        if (mSplitQueryDao != null) {
            return mSplitQueryDao;
        }
        synchronized (this) {
            mSplitQueryDao = new SplitQueryDaoImpl(this);
            return mSplitQueryDao;
        }
    }
}