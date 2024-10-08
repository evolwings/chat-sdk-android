package sdk.chat.core.session;

import static sdk.chat.core.dao.DaoCore.daoSession;
import static sdk.chat.core.dao.DaoCore.fetchEntityWithProperty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.greendao.query.Join;
import org.greenrobot.greendao.query.QueryBuilder;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import sdk.chat.core.dao.DaoCore;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.MessageDao;
import sdk.chat.core.dao.PublicKey;
import sdk.chat.core.dao.ReadReceiptUserLink;
import sdk.chat.core.dao.ReadReceiptUserLinkDao;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.ThreadDao;
import sdk.chat.core.dao.User;
import sdk.chat.core.dao.UserThreadLink;
import sdk.chat.core.dao.UserThreadLinkDao;
import sdk.chat.core.interfaces.CoreEntity;
import sdk.chat.core.types.ReadStatus;
import sdk.guru.common.Optional;
import sdk.guru.common.RX;

/**
 * Created by benjaminsmiley-andrews on 03/05/2017.
 */

public class StorageManager {

    protected Map<String, CoreEntity> entityCache = new HashMap<>();
    protected boolean entityCacheEnabled = false;

    public List<Thread> fetchThreadsForCurrentUser() {
        Logger.debug(java.lang.Thread.currentThread().getName());

        List<Thread> threads = new ArrayList<>();

        List<UserThreadLink> links = DaoCore.fetchEntitiesWithProperty(UserThreadLink.class, UserThreadLinkDao.Properties.UserId, ChatSDK.currentUser().getId());

        for (UserThreadLink link : links) {
            Thread thread = link.getThread();
            if (thread != null) {
                threads.add(thread);
            } else {
                // Delete the link - it's obviously corrupted
                link.delete();
            }
        }

        return threads;
    }

    public Single<List<Thread>> fetchThreadsForCurrentUserAsync() {
        return Single.defer(() -> Single.just(fetchThreadsForCurrentUser())).subscribeOn(RX.db());
    }

    public ReadReceiptUserLink readReceipt(Long messageId, Long userId) {
        Logger.debug(java.lang.Thread.currentThread().getName());

        QueryBuilder<ReadReceiptUserLink> queryBuilder = daoSession.queryBuilder(ReadReceiptUserLink.class);
        queryBuilder.where(ReadReceiptUserLinkDao.Properties.UserId.eq(userId)).where(ReadReceiptUserLinkDao.Properties.MessageId.eq(messageId));
        List<ReadReceiptUserLink> links = queryBuilder.list();

        if (links.size() > 1) {
            Logger.debug("Multiple read receipts for one user");
            for (int i = 1; i < links.size(); i++) {
                links.get(i).delete();
            }
        }

        if (!links.isEmpty()) {
            return links.get(0);
        }

        return null;
    }

    public Single<Optional<ReadReceiptUserLink>> readReceiptAsync(Long messageId, Long userId) {
        return Single.defer(() -> Single.just(new Optional<>(readReceipt(messageId, userId)))).subscribeOn(RX.db());
    }

    public Message fetchOrCreateMessageWithEntityID(String entityId) {
        return fetchOrCreateEntityWithEntityID(Message.class, entityId);
    }

    public synchronized Message fetchMessageWithEntityID(String entityId) {
        return fetchEntityWithEntityID(entityId, Message.class);
    }

    public synchronized <T extends CoreEntity> T fetchOrCreateEntityWithEntityID(Class<T> c, String entityId) {
        Logger.debug(java.lang.Thread.currentThread().getName());

        if (entityCacheEnabled && entityCache.containsKey(c.toString() + entityId)) {
            return (T) entityCache.get(c + entityId);
        }

        T entity = DaoCore.fetchEntityWithEntityID(c, entityId);
//
        if (entity == null) {
            entity = DaoCore.getEntityForClass(c);
            entity.setEntityID(entityId);
            entity = DaoCore.createEntity(entity);

            if (entityCacheEnabled) {
                entityCache.put(c.toString() + entityId, entity);
            }
        }

        return entity;
    }

    public <T extends CoreEntity> Single<T> fetchOrCreateEntityWithEntityIDAsync(Class<T> c, String entityId) {
        return Single.defer(() -> Single.just(fetchOrCreateEntityWithEntityID(c, entityId))).subscribeOn(RX.db());
    }

    public synchronized <T> T createEntity(Class<T> c) {
        Logger.debug(java.lang.Thread.currentThread().getName());
        T entity = DaoCore.getEntityForClass(c);
        DaoCore.createEntity(entity);
        if (entity instanceof Thread) {
            ((Thread) entity).setUserAccountID(ChatSDK.currentUserID());
            ((Thread) entity).update();
        }
        return entity;
    }

    public <T> Single<T> createEntityAsync(Class<T> c) {
        return Single.defer(() -> Single.just(createEntity(c)).subscribeOn(RX.db()));
    }

    public <T extends CoreEntity> T insertOrReplaceEntity(T entity) {
        DaoCore.createEntity(entity);
        return entity;
    }

    public <T extends CoreEntity> Single<T> insertOrReplaceEntityAsync(T entity) {
        return Single.defer(() -> Single.just(insertOrReplaceEntity(entity)).subscribeOn(RX.db()));
    }

    public synchronized <T extends CoreEntity> T fetchEntityWithEntityID(String entityID, Class<T> c) {
        if (entityCacheEnabled && entityCache.containsKey(c.toString() + entityID)) {
            return (T) entityCache.get(c + entityID);
        }
        if (c == Thread.class) {
            return (T) fetchThreadWithEntityID(entityID);
        }
        return DaoCore.fetchEntityWithEntityID(c, entityID);
    }

    public <T extends CoreEntity> Single<T> fetchEntityWithEntityIDAsync(String entityID, Class<T> c) {
        return Single.defer(() -> Single.just(fetchEntityWithEntityID(entityID, c)).subscribeOn(RX.db()));
    }

    public synchronized User fetchUserWithEntityID(String entityID) {
        return DaoCore.fetchEntityWithEntityID(User.class, entityID);
    }

    public Single<User> fetchUserWithEntityIDAsync(String entityID) {
        return Single.defer(() -> Single.just(fetchUserWithEntityID(entityID)).subscribeOn(RX.db()));
    }

    @NonNull
    public List<Thread> fetchThreadsWithType(int type) {
        String currentUser = ChatSDK.currentUserID();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        QueryBuilder<Thread> qb = DaoCore.daoSession.queryBuilder(Thread.class);
        qb.where(ThreadDao.Properties.Type.eq(type));
        qb.where(ThreadDao.Properties.UserAccountID.eq(currentUser));
        return qb.list();
    }

    public synchronized Thread fetchOrCreateThreadWithEntityID(@NonNull String entityId) {
        Logger.debug(java.lang.Thread.currentThread().getName());

        Thread thread = fetchThreadWithEntityID(entityId);

        if (thread == null) {
            thread = createEntity(Thread.class);
            thread.setEntityID(entityId);
            thread.update();
        }

        return thread;
    }

    @Nullable
    public synchronized Thread fetchThreadWithEntityID(@NonNull String entityID) {
        String currentUser = ChatSDK.currentUserID();
        if (currentUser == null) {
            return null;
        }
        QueryBuilder<Thread> qb = DaoCore.daoSession.queryBuilder(Thread.class);
        qb.where(ThreadDao.Properties.EntityID.eq(entityID));
        qb.where(ThreadDao.Properties.UserAccountID.eq(currentUser));
        List<Thread> threads = qb.list();
        return !threads.isEmpty() ? threads.get(0) : null;
    }


    public Single<List<Thread>> fetchThreadsWithTypeAsync(int type) {
        return Single.defer(() -> Single.just(fetchThreadsWithType(type)).subscribeOn(RX.db()));
    }

    @NonNull
    public List<Message> fetchUnreadMessagesForThread(Long threadId) {
        if (threadId == null) {
            return Collections.emptyList();
        }

        return createUnreadMessagesQuery(threadId).list();
    }

    public long fetchUnreadMessagesCountForThread(Long threadId) {
        if (threadId == null) {
            return 0;
        }

        return createUnreadMessagesQuery(threadId).count();
    }

    @NonNull
    private QueryBuilder<Message> createUnreadMessagesQuery(long threadId) {
        Logger.debug(java.lang.Thread.currentThread().getName());

        Long currentUserId = ChatSDK.currentUser().getId();

        QueryBuilder<Message> qb = daoSession.queryBuilder(Message.class);
        Join<Message, ReadReceiptUserLink> join = qb
                .where(qb.and(MessageDao.Properties.ThreadId.eq(threadId), MessageDao.Properties.SenderId.notEq(currentUserId)))
                .join(ReadReceiptUserLink.class, ReadReceiptUserLinkDao.Properties.MessageId);

        join.where(join.and(ReadReceiptUserLinkDao.Properties.UserId.eq(currentUserId), ReadReceiptUserLinkDao.Properties.Status.notEq(ReadStatus.Read)));

        return qb;
    }

    public Single<List<Message>> fetchUnreadMessagesForThreadAsync(Long threadId) {
        return Single.defer(() -> Single.just(fetchUnreadMessagesForThread(threadId)).subscribeOn(RX.db()));
    }

//    public int fetchUnreadMessageCount(ThreadType threadType) {
//        Long currentUserId = ChatSDK.currentUser().getId();
//
//        QueryBuilder<ReadReceiptUserLink> qb = daoSession.queryBuilder(ReadReceiptUserLink.class);
//        Join<?, Message> message = qb.join(Message.class, ReadReceiptUserLinkDao.Properties.MessageId);
//
//        Join<Message, ReadReceiptUserLink> rrj;
//
//        Join<ReadReceiptUserLink, Thread> thread = qb.join(rrj, null, Thread.class, null);
//
//
//        QueryBuilder<Thread> qb2 = daoSession.queryBuilder(Thread.class);
//        Join<Thread, Message> join = qb2.join(Message.class, MessageDao.Properties.ThreadId);
//        Join<Message, ReadReceiptUserLink> join2 = qb2.join(join, MessageDao.Properties.Id, ReadReceiptUserLink.class, ReadReceiptUserLinkDao.Properties.MessageId);
//
//        Join message = qb.join(Message.class, MessageDao.Properties.ThreadId).where(MessageDao.Properties.SenderId.notEq(currentUserId));
//
//        Join<Message, ReadReceiptUserLink> readReceipt = qb.join(rrj, MessageDao.Properties., ReadReceiptUserLink.class, ReadReceiptUserLinkDao.Properties.MessageId);
//
//        join.where(join.and(ReadReceiptUserLinkDao.Properties.UserId.eq(currentUserId), ReadReceiptUserLinkDao.Properties.Status.notEq(ReadStatus.Read)));
//        return qb.list().size();
//    }

    public Thread fetchThreadWithID(long threadID) {
        return fetchEntityWithProperty(Thread.class, ThreadDao.Properties.Identifier, threadID);
    }

    public Single<Thread> fetchThreadWithIDAsync(long threadID) {
        return Single.defer(() -> Single.just(fetchThreadWithID(threadID)).subscribeOn(RX.db()));
    }

//    public @Nullable Thread fetchThreadWithEntityID (String entityID) {
//        Logger.debug(java.lang.Thread.currentThread().getName());
//        if(entityID != null) {
//            return fetchEntityWithProperty(Thread.class, ThreadDao.Properties.EntityID, entityID);
//        }
//        return null;
//    }

    public Single<Thread> fetchThreadWithEntityIDAsync(String entityID) {
        return Single.defer(() -> Single.just(fetchThreadWithEntityID(entityID)).subscribeOn(RX.db()));
    }

    public Thread fetchThreadWithUsers(List<User> users) {
        Logger.debug(java.lang.Thread.currentThread().getName());
        for (Thread t : allThreads()) {
            if (t.getUsers().equals(users)) {
                return t;
            }
        }
        return null;
    }

    public Single<Thread> fetchThreadWithUsersAsync(List<User> users) {
        return Single.defer(() -> Single.just(fetchThreadWithUsers(users)).subscribeOn(RX.db()));
    }

    public List<Thread> allThreads() {
        Logger.debug(java.lang.Thread.currentThread().getName());

        List<UserThreadLink> links = DaoCore.fetchEntitiesWithProperty(UserThreadLink.class, UserThreadLinkDao.Properties.UserId, ChatSDK.currentUser().getId());
        ArrayList<Thread> threads = new ArrayList<>();
        for (UserThreadLink link : links) {
            threads.add(link.getThread());
        }

        return threads;
    }

    public Single<List<Thread>> allThreadsAsync() {
        return Single.defer(() -> Single.just(allThreads()).subscribeOn(RX.db()));
    }


    public List<Message> fetchMessagesForThreadWithID(long threadID, @Nullable Date from, @Nullable Date to, int limit) {
        Logger.debug(java.lang.Thread.currentThread().getName());

        // If we have a zero date, treat it as null
        if (to != null && to.equals(new Date(0))) {
            to = null;
        }
        if (from != null && from.equals(new Date(0))) {
            from = null;
        }

        QueryBuilder<Message> qb = daoSession.queryBuilder(Message.class);
        qb.where(MessageDao.Properties.ThreadId.eq(threadID));

        // Making sure no null messages infected the sort.
        qb.where(MessageDao.Properties.Date.isNotNull());
        qb.where(MessageDao.Properties.SenderId.isNotNull());

        if (to != null) {
            qb.where(MessageDao.Properties.Date.lt(to.getTime()));
        }
        if (from != null) {
            qb.where(MessageDao.Properties.Date.gt(from.getTime()));
        }

        qb.orderDesc(MessageDao.Properties.Date);

        if (limit > 0) {
            qb.limit(limit);
        }

        List<Message> list = qb.list();

        return list;
    }

    public List<Message> fetchMessagesWithFailedDecryption() {
        Logger.debug(java.lang.Thread.currentThread().getName());

//        QueryBuilder<Thread> qb = daoSession.queryBuilder(Thread.class);
//        qb.where(ThreadDao.Properties.UserAccountID.eq(ChatSDK.currentUserID()))
//                .join(Message.class, MessageDao.Properties.ThreadId).where(MessageDao.Properties.DecryptionFailed.eq(true));


//        return qb.list();

        QueryBuilder<Message> qb = daoSession.queryBuilder(Message.class);
        qb.where(MessageDao.Properties.EncryptedText.isNotNull())
                .join(MessageDao.Properties.ThreadId, Thread.class)
                .where(ThreadDao.Properties.UserAccountID.eq(ChatSDK.currentUserID()));
        return qb.list();

//        qb.where(MessageDao.Properties.ThreadId.eq(threadID));
//        qb.where(MessageDao.Properties.DecryptionFailed.eq(true));
//
//        QueryBuilder<Message> qb = daoSession.queryBuilder(Message.class);
//
//        Join<Message, Thread> join = qb.where(qb.and(MessageDao.Properties.ThreadId.eq(threadId), MessageDao.Properties.SenderId.notEq(currentUserId)))
//                .join(ReadReceiptUserLink.class, ReadReceiptUserLinkDao.Properties.MessageId);
//
//        return qb.list();
    }

    public PublicKey getPublicKey(String userId) {
        return fetchEntityWithEntityID(userId, PublicKey.class);
    }

    public void deletePublicKey(String userId) {
        PublicKey key = getPublicKey(userId);
        if (key != null) {
            DaoCore.deleteEntity(key);
        }
    }

    public void deleteAllPublicKeys() {
        List<PublicKey> keys = DaoCore.fetchEntitiesOfClass(PublicKey.class);
        for (PublicKey key : keys) {
            DaoCore.deleteEntity(key);
        }
    }

    public void addPublicKey(String userId, String identifier, String key) {
        PublicKey publicKey = fetchOrCreateEntityWithEntityID(PublicKey.class, userId);
        publicKey.setKey(key);
        publicKey.setIdentifier(identifier);
        DaoCore.updateEntity(publicKey);
    }

    public Single<List<Message>> fetchMessagesForThreadWithIDAsync(long threadID, Date from, Date to, int limit) {
        return Single.defer(() -> Single.just(fetchMessagesForThreadWithID(threadID, from, to, limit)).subscribeOn(RX.db()));
    }

    public void update(CoreEntity entity) {
        DaoCore.updateEntity(entity);
    }

    public Completable updateAsync(CoreEntity entity) {
        return Completable.defer(() -> {
            update(entity);
            return Completable.complete();
        }).subscribeOn(RX.db());
    }

    public void delete(Object entity) {
        DaoCore.deleteEntity(entity);
    }

    public Completable deleteAsync(Object entity) {
        return Completable.defer(() -> {
            delete(entity);
            return Completable.complete();
        }).subscribeOn(RX.db());
    }
}
