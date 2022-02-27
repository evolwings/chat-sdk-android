package sdk.chat.core.dao;

import androidx.annotation.Nullable;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.JoinEntity;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.OrderBy;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;
import sdk.chat.core.R;
import sdk.chat.core.base.AbstractEntity;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.StringChecker;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS
// KEEP INCLUDES - put your custom includes here

@Entity
public class Thread extends AbstractEntity {

    @Id private Long id;

    @Unique
    private String entityID;

    private Date creationDate;
    private Integer type;
    private Long creatorId;
    private Date loadMessagesFrom;
    private Boolean deleted;
    private String draft;
    private Date canDeleteMessagesFrom;

    @ToOne(joinProperty = "creatorId")
    private User creator;

    @ToMany
    @JoinEntity(
            entity = UserThreadLink.class,
            sourceProperty = "threadId",
            targetProperty = "userId"
    )
    private List<UserThreadLink> userThreadLinks;

    @ToMany(referencedJoinProperty = "threadId")
    private List<ThreadMetaValue> metaValues;

    @ToMany(referencedJoinProperty = "threadId")
    @OrderBy("date ASC")
    private List<Message> messages;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 473811190)
    private transient ThreadDao myDao;
    @Generated(hash = 1767171241)
    private transient Long creator__resolvedKey;
    public Thread() {
    }

    public Thread(Long id) {
        this.id = id;
    }

    @Generated(hash = 2012841452)
    public Thread(Long id, String entityID, Date creationDate, Integer type, Long creatorId, Date loadMessagesFrom, Boolean deleted, String draft, Date canDeleteMessagesFrom) {
        this.id = id;
        this.entityID = entityID;
        this.creationDate = creationDate;
        this.type = type;
        this.creatorId = creatorId;
        this.loadMessagesFrom = loadMessagesFrom;
        this.deleted = deleted;
        this.draft = draft;
        this.canDeleteMessagesFrom = canDeleteMessagesFrom;


    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<User> getUsers() {

        List<UserThreadLink> links = ChatSDK.db().getDaoCore().fetchEntitiesWithProperty(UserThreadLink.class, UserThreadLinkDao.Properties.ThreadId, getId());
        if (links == null) {
            return Collections.emptyList();
        }

        Set<User> users  = new HashSet<>();

        for (UserThreadLink link: links) {
            User user = link.getUser();
            if (user != null) {
                users.add(user);
            }
        }

        return new ArrayList<>(users);
    }

    /**
     * Return a list of users who haven't left the group and are not banned
     * @return
     */
    public List<User> getMembers() {
        List<UserThreadLink> links = getLinks();
        if (links == null) {
            return Collections.emptyList();
        }

        Set<User> users = new HashSet<>();

        for (UserThreadLink link: links) {
            User user = link.getUser();
            if (user != null && !user.isMe() && !link.hasLeft() && !link.isBanned()) {
                users.add(user);
            }
        }

        return new ArrayList<>(users);
    }

    public List<UserThreadLink> getLinks() {
        return ChatSDK.db().getDaoCore().fetchEntitiesWithProperty(UserThreadLink.class, UserThreadLinkDao.Properties.ThreadId, getId());
    }

    public boolean containsUser (User user) {
        for(User u : getUsers()) {
            if (u.equalsEntity(user)) {
                return true;
            }
        }
        return false;
    }

    public Date orderDate() {
        Date date = lastMessageAddedDate();
        if (date == null) {
            date = creationDate;
        }
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public Date lastMessageAddedDate(){
        Message lastMessage = lastMessage();
        if (lastMessage != null) {
            return lastMessage.getDate();
        }
        return null;
    }


    public boolean addUser(User user) {
        return addUser(user, true);
    }

    public boolean addUser(User user, boolean notify) {
        UserThreadLink link = getUserThreadLink(user.getId());
        if (ChatSDK.db().getDaoCore().connectUserAndThread(user, this) || link != null && link.hasLeft()) {
            if (link == null) {
                link = getUserThreadLink(user.getId());
            }
            if (link != null && link.setHasLeft(false) && notify) {
                ChatSDK.events().source().accept(NetworkEvent.threadUserAdded(this, user));
            }
        }
        return false;
    }

    public void removeUser(User user) {
        removeUser(user, true);
    }

    public void removeUser(User user, boolean notify) {
        if(ChatSDK.db().getDaoCore().breakUserAndThread(user, this) && notify) {
            ChatSDK.events().source().accept(NetworkEvent.threadUserRemoved(this, user));
        }
//        user.update();
//        update();
    }

    public User otherUser () {
        if (getUsers().size() == 2) {
            for (User u : getUsers()) {
                if (!u.isMe()) {
                    return u;
                }
            }
        }
        return null;
    }

    public void addUsers (User... users) {
        addUsers(Arrays.asList(users));
    }

    public void addUsers (List<User> users) {
        for(User u : users) {
            addUser(u);
        }
    }

    public void removeUsers (List<User> users) {
        for(User u : users) {
            removeUser(u);
        }
    }

    public boolean containsMessageWithID (String messageEntityID) {
        return getMessageWithEntityID(messageEntityID) != null;
    }

    public Message getMessageWithEntityID (String messageEntityID) {
        for(Message m : getMessages()) {
            if(m.getEntityID() != null && messageEntityID != null && m.equalsEntityID(messageEntityID)) {
                return m;
            }
        }
        return null;
    }

    public void removeUsers (User... users) {
        removeUsers(Arrays.asList(users));
    }

    /** Fetch messages list from the db for current thread, Messages will be order Desc/Asc on demand.*/
    public List<Message> getMessagesWithOrder(int order){
        return getMessagesWithOrder(order, 0);
    }

    /** Fetch messages list from the db for current thread, Messages will be order Desc/Asc on demand.*/
    @Keep
    public List<Message> getMessagesWithOrder(int order, int limit) {
        QueryBuilder<Message> qb = daoSession.queryBuilder(Message.class);
        qb.where(MessageDao.Properties.ThreadId.eq(getId()));

        if(order == DaoCore.ORDER_ASC) {
            qb.orderAsc(MessageDao.Properties.Date);
        }
        else if(order == DaoCore.ORDER_DESC) {
            qb.orderDesc(MessageDao.Properties.Date);
        }

        // Making sure no null messages infected the sort.
        qb.where(MessageDao.Properties.Date.isNotNull());

        if (limit > 0) {
            qb.limit(limit);
        }

        Query<Message> query = qb.build().forCurrentThread();
        return query.list();
    }
    public Single<List<Message>> getMessagesWithOrderAsync(int order, int limit) {
        return ThreadAsync.getMessagesWithOrderAsync(this, order, limit);
    }

    public void addMessage(Message message) {
        addMessage(message, true);
    }

    public void addMessage(Message message, boolean notify) {
        message.setThreadId(this.getId());
        List<Message> messages = getMessages();
        if (!messages.contains(message)) {
            if (!messages.isEmpty()) {
                Message previousMessage = messages.get(messages.size() - 1);
                previousMessage.setNextMessage(message);
                previousMessage.update();
                message.setPreviousMessage(previousMessage);
                if (notify) {
                    ChatSDK.events().source().accept(NetworkEvent.messageUpdated(previousMessage));
                }
            }
            getMessages().add(message);
            message.update();
            update();
//            refresh();
            if (notify) {
                ChatSDK.events().source().accept(NetworkEvent.messageAdded(message));
            }
        }
    }

    public void setMetaValue(String key, Object value) {
        setMetaValue(key, value, true);
    }

    public void setMuted(boolean muted) {
        if (muted) {
            setMetaValue(Keys.Mute, "");
        } else {
            removeMetaValue(Keys.Mute);
        }
    }

    public boolean isMuted() {
        return metaValueForKey(Keys.Mute) != null;
    }

    @Keep
    public void setMetaValue(String key, Object value, boolean notify) {
        ThreadMetaValue metaValue = metaValueForKey(key);

        if (metaValue == null || metaValue.getValue() == null || !metaValue.getValue().equals(value)) {
            if (metaValue == null) {
                metaValue = ChatSDK.db().create(ThreadMetaValue.class);
                metaValue.setThreadId(this.getId());
                getMetaValues().add(metaValue);
            }

            metaValue.setValue(value);
            metaValue.setKey(key);
            metaValue.update();
            update();

            if (notify) {
                ChatSDK.events().source().accept(NetworkEvent.threadMetaUpdated(this));
            }
        }
    }

    @Keep
    public void setMetaValues(Map<String, Object> values) {
        setMetaValues(values, true);
    }

    public void setMetaValues(Map<String, Object> values, boolean notify) {
        Map<String, Object> current = metaMap();
        if (values != null && !current.entrySet().equals(values.entrySet())) {

            // Remove any deleted values
            for (String key: current.keySet()) {
                if (!values.containsKey(key)) {
                    removeMetaValue(key);
                }
            }

            for (String key : values.keySet()) {
                this.setMetaValue(key, values.get(key), false);
            }

            if (notify) {
                ChatSDK.events().source().accept(NetworkEvent.threadMetaUpdated(this));
            }
        }
    }

    public Map<String, Object> metaMap() {
        HashMap<String, Object> map = new HashMap<>();

        for(ThreadMetaValue v : getMetaValues()) {
            map.put(v.getKey(), v.getValue());
        }

        return map;
    }


    @Keep
    public void removeMetaValue (String key) {
        ThreadMetaValue metaValue = metaValueForKey(key);

        if (metaValue != null) {
            metaValues.remove(metaValue);
            metaValue.delete();
            resetMetaValues();
            update();
        }

    }

    @Keep
    public void updateValues (Map<String, Object> values) {
        for (String key : values.keySet()) {
            setMetaValue(key, values.get(key));
        }
    }

    @Keep
    public ThreadMetaValue metaValueForKey (String key) {
        return MetaValueHelper.metaValueForKey(key, getMetaValues());
    }

    public void removeMessage(Message message) {
        removeMessage(message, true);
    }

    public void removeMessage(Message message, boolean notify) {

        List<Message> messages = getMessages();
        int indexOfMessage = messages.indexOf(message);
        if (indexOfMessage >= 0) {
            Message previousMessage = null;
            Message nextMessage = null;

            // If it's not the first text
            if (indexOfMessage > 0) {
                previousMessage = messages.get(indexOfMessage - 1);
            }
            // If it's not the last text
            if (indexOfMessage < messages.size() - 1) {
                nextMessage = messages.get(indexOfMessage + 1);
            }
            if (previousMessage != null) {
                previousMessage.setNextMessage(nextMessage);
                previousMessage.update();
                if (notify) {
                    ChatSDK.events().source().accept(NetworkEvent.messageUpdated(previousMessage));
                }
            }
            if (nextMessage != null) {
                nextMessage.setPreviousMessage(previousMessage);
                nextMessage.update();
                if (notify) {
                    ChatSDK.events().source().accept(NetworkEvent.messageUpdated(nextMessage));
                }
            }

            messages.remove(message);
        }

        message.cascadeDelete();

        update();
        resetMessages();

        if(notify) {
            ChatSDK.events().source().accept(NetworkEvent.messageRemoved(message));
        }
    }

    public boolean hasUser(User user) {
        UserThreadLink data = ChatSDK.db().getDaoCore().fetchEntityWithProperties(UserThreadLink.class,
                        new Property[]{UserThreadLinkDao.Properties.ThreadId, UserThreadLinkDao.Properties.UserId}, getId(), user.getId());

        return data != null;
    }

    public int getUnreadMessagesCount() {
        return ChatSDK.db().fetchUnreadMessagesForThread(getId()).size();
//
//        int count = 0;
//        List<Message> messages = getMessagesWithOrder(DaoCore.ORDER_DESC);
//        for (Message m : messages)
//        {
//            if(!m.isRead())
//                count++;
//            else break;
//        }
//
//        return count;
    }

    public boolean isLastMessageWasRead(){
        List<Message> messages = getMessagesWithOrder(DaoCore.ORDER_DESC);
        return messages == null || messages.size() == 0 || messages.get(0).isRead();
    }

    public boolean isDeleted() {
        return deleted != null && deleted;
    }

    public Completable markReadAsync() {
        return ThreadAsync.markRead(this);
    }

    public void markRead() {
        List<Message> unreadMessages = ChatSDK.db().fetchUnreadMessagesForThread(getId());
        for(Message m : unreadMessages) {
            m.markRead();
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityID() {
        return this.entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public java.util.Date getCreationDate() {
        return this.creationDate;
    }

    public void setCreationDate(java.util.Date creationDate) {
        this.creationDate = creationDate;
    }

    public Boolean getDeleted() {
        return isDeleted();
    }

    public void setDeleted(boolean deleted) {
        setDeleted(deleted, true);
    }

    public void setDeleted(boolean deleted, boolean notify) {
        if (this.deleted == null || this.deleted != deleted) {
            this.deleted = deleted;
            update();
            if (notify) {
                if (deleted) {
                    ChatSDK.events().source().accept(NetworkEvent.threadRemoved(this));
                } else {
                    ChatSDK.events().source().accept(NetworkEvent.threadAdded(this));
                }
            }
        }
    }

    public String getName() {
        ThreadMetaValue metaValue = metaValueForKey(Keys.Name);
        if (metaValue != null) {
            return metaValue.getStringValue();
        }
        return null;
    }

    public String getDisplayName () {
        // Either get the name or return the names of the participants
        if(!StringChecker.isNullOrEmpty(getName())) {
            return getName();
        }
        else {
            return getUserListString();
        }
    }

    public String getUserListString () {
        StringBuilder name = new StringBuilder();
        for(User u : getMembers()) {
            if(!u.isMe() && !StringChecker.isNullOrEmpty(u.getName())) {
                name.append(u.getName()).append(", ");
            }
        }
        if(name.length() >= 2) {
            name = new StringBuilder(name.substring(0, name.length() - 2));
        }
        return name.toString();
    }

    public void setName(String name) {
        setName(name, true);
    }

    public void setName(String name, boolean notify) {
        setMetaValue(Keys.Name, name, notify);
    }

    public Date getLastMessageAddedDate () {
        Message lastMessage = lastMessage();
        if(lastMessage != null && lastMessage.getDate() != null) {
            return lastMessage.getDate();
        }
        return null;
    }

    public Integer getType() {
        return this.type;
    }

    public boolean typeIs(int value) {
        return getType() != null && (getType() & value) > 0;
    }

    public boolean typeOr (int value) {
        return getType() != null && (getType() | value) > 0;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Message lastMessage () {
        List<Message> messages = getMessagesWithOrder(DaoCore.ORDER_DESC, 1);
        if (messages.size() > 0) {
            return messages.get(0);
        } else {
            return null;
        }
    }

    public Long getCreatorId() {
        return this.creatorId;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 2088804448)
    public User getCreator() {
        Long __key = this.creatorId;
        if (creator__resolvedKey == null || !creator__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            UserDao targetDao = daoSession.getUserDao();
            User creatorNew = targetDao.load(__key);
            synchronized (this) {
                creator = creatorNew;
                creator__resolvedKey = __key;
            }
        }
        return creator;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 501133931)
    public void setCreator(User creator) {
        synchronized (this) {
            this.creator = creator;
            creatorId = creator == null ? null : creator.getId();
            creator__resolvedKey = creatorId;
        }
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 846992604)
    public List<UserThreadLink> getUserThreadLinks() {
        if (userThreadLinks == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            UserThreadLinkDao targetDao = daoSession.getUserThreadLinkDao();
            List<UserThreadLink> userThreadLinksNew = targetDao._queryThread_UserThreadLinks(id);
            synchronized (this) {
                if (userThreadLinks == null) {
                    userThreadLinks = userThreadLinksNew;
                }
            }
        }
        return userThreadLinks;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 180413695)
    public synchronized void resetUserThreadLinks() {
        userThreadLinks = null;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 581641122)
    public List<Message> getMessages() {
        if (messages == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            MessageDao targetDao = daoSession.getMessageDao();
            List<Message> messagesNew = targetDao._queryThread_Messages(id);
            synchronized (this) {
                if (messages == null) {
                    messages = messagesNew;
                }
            }
        }
        return messages;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1942469556)
    public synchronized void resetMessages() {
        messages = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 5320433)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getThreadDao() : null;
    }

    public String getImageUrl() {
        ThreadMetaValue metaValue = metaValueForKey(Keys.ImageUrl);
        if (metaValue != null) {
            return metaValue.getStringValue();
        }
        return null;
    }

    public void setImageUrl(String imageUrl) {
        setImageUrl(imageUrl, true);
    }

    public void setImageUrl(String imageUrl, boolean notify) {
        setMetaValue(Keys.ImageUrl, imageUrl, notify);
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 132417041)
    public List<ThreadMetaValue> getMetaValues() {
        if (metaValues == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            ThreadMetaValueDao targetDao = daoSession.getThreadMetaValueDao();
            List<ThreadMetaValue> metaValuesNew = targetDao._queryThread_MetaValues(id);
            synchronized (this) {
                if (metaValues == null) {
                    metaValues = metaValuesNew;
                }
            }
        }
        return metaValues;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 365870950)
    public synchronized void resetMetaValues() {
        metaValues = null;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    @Nullable
    public UserThreadLink getUserThreadLink(Long userId) {
        return ChatSDK.db().getDaoCore().fetchEntityWithProperties(UserThreadLink.class, new Property[] {UserThreadLinkDao.Properties.ThreadId, UserThreadLinkDao.Properties.UserId}, getId(), userId);
    }

    public void setPermission(String userEntityID, String permission) {
        setPermission(userEntityID, permission, true, ChatSDK.config().sendSystemMessageWhenRoleChanges);
    }

    public void setPermission(String userEntityID, String permission, boolean notify, boolean sendSystemMessage) {
        User user = ChatSDK.db().fetchUserWithEntityID(userEntityID);
        if (user != null) {
            UserThreadLink link = getUserThreadLink(user.getId());
            if (link != null) {
                if (link.setAffiliation(permission) && notify) {
                    Logger.info("Set Affiliation " + userEntityID + " " + permission);
                    ChatSDK.events().source().accept(NetworkEvent.threadUsersRoleUpdated(this, user));
                    if (sendSystemMessage && user.isMe() && typeIs(ThreadType.Group)) {
                        String message = String.format(ChatSDK.getString(R.string.role_changed_to__), ChatSDK.thread().localizeRole(permission));
                        ChatSDK.thread().sendLocalSystemMessage(message, this);
                    }
                }
            }
        }
    }

    public String getPermission(String userEntityID) {
        User user = ChatSDK.db().fetchUserWithEntityID(userEntityID);
        if (user != null) {
            UserThreadLink link = getUserThreadLink(user.getId());
            if (link != null) {
                return link.getAffiliation();
            }
        }
        return null;
    }

    public Date getLoadMessagesFrom() {
        return this.loadMessagesFrom;
    }

    public void setLoadMessagesFrom(Date loadMessagesFrom) {
        this.loadMessagesFrom = loadMessagesFrom;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getDraft() {
        return this.draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
        update();
    }

    public int indexOfFirstDeletableMessage() {
        List<Message> messages = getMessages();
        if (messages == null || messages.isEmpty()) {
            return -1;
        }
        int index = messages.size() - ChatSDK.config().messageDeletionListenerLimit;
        if (index < 0) {
            return 0;
        }
        return index;
    }

    public int indexOf(Message message) {
        return messages.indexOf(message);
    }

    public Date getCanDeleteMessagesFrom() {
        return this.canDeleteMessagesFrom;
    }

    public void setCanDeleteMessagesFrom(Date canDeleteMessagesFrom) {
        this.canDeleteMessagesFrom = canDeleteMessagesFrom;
        update();
    }

    @Keep
    public boolean isReadOnly() {
        ThreadMetaValue value = metaValueForKey(Keys.ReadOnly);
        if (value != null) {
            if (value.getBooleanValue() != null) {
                return value.getBooleanValue();
            }
            // Deprecated. To cover old case where we set this to a string
            if(value.getStringValue() != null) {
                return true;
            }
        }
        return false;
    }

    public Long getWeight() {
        Object value = metaMap().get(Keys.Weight);
        if (value instanceof Long) {
            return (Long) value;
        }
        return Long.MAX_VALUE;
    }

    public void cascadeDelete() {
        removeMessagesAndMarkDeleted();
        List<UserThreadLink> links = new ArrayList<>(getLinks());
        for (UserThreadLink link: links) {
            link.cascadeDelete();
        }
        List<ThreadMetaValue> values = new ArrayList<>(getMetaValues());
        for (ThreadMetaValue value: values) {
            value.delete();
        }
        delete();
    }

    public void removeMessagesAndMarkDeleted() {
        List<Message> messages = new ArrayList<>(getMessages());
        for (Message message: messages) {
            removeMessage(message);
            message.cascadeDelete();
        }
        setDeleted(true);
        setLoadMessagesFrom(new Date());
        update();
    }

}
