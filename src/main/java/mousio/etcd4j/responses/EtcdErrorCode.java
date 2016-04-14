package mousio.etcd4j.responses;

/**
 * Etcd Error Code
 *
 * @see <a href="https://github.com/coreos/etcd/blob/master/error/error.go">error.go</a>
 */
public class EtcdErrorCode {

    // command related errors
    public static final int KeyNotFound          = 100; // Key not found
    public static final int TestFailed           = 101; // Compare failed
    public static final int NotFile              = 102; // Not a file
    public static final int NoMorePeer           = 103; // Reached the max number of peers in the cluster
    public static final int NotDir               = 104; // Not a directory
    public static final int NodeExist            = 105; // Key already exists
    public static final int KeyIsPreserved       = 106; // The prefix of given key is a keyword in etcd
    public static final int RootROnly            = 107; // Root is read only
    public static final int DirNotEmpty          = 108; // Directory not empty
    public static final int ExistingPeerAddr     = 109; // Peer address has existed
    public static final int Unauthorized         = 110; // The request requires user authentication

    // Post form related errors
    public static final int ValueRequired        = 200; // Value is Required in POST form
    public static final int PrevValueRequired    = 201; // PrevValue is Required in POST form
    public static final int TTLNaN               = 202; // The given TTL in POST form is not a number
    public static final int IndexNaN             = 203; // The given index in POST form is not a number
    public static final int ValueOrTTLRequired   = 204; // Value or TTL is required in POST form
    public static final int TimeoutNaN           = 205; // The given timeout in POST form is not a number
    public static final int NameRequired         = 206; // Name is required in POST form
    public static final int IndexOrValueRequired = 207; // Index or value is required
    public static final int IndexValueMutex      = 208; // Index and value cannot both be specified
    public static final int InvalidField         = 209; // Invalid field
    public static final int InvalidForm          = 210; // Invalid POST form
    public static final int RefreshValue         = 211; // Value provided on refresh
    public static final int RefreshTTLRequired   = 212; // A TTL must be provided on refresh

    // raft related errors
    public static final int RaftInternal         = 300; // Raft Internal Error
    public static final int LeaderElect          = 301; // During Leader Election

    // etcd related errors
    public static final int WatcherCleared       = 400; // watcher is cleared due to etcd recovery
    public static final int EventIndexCleared    = 401; // The event in requested index is outdated and cleared
    public static final int StandbyInternal      = 402; // Standby Internal Error
    public static final int InvalidActiveSize    = 403; // Invalid active size
    public static final int InvalidRemoveDelay   = 404; // Standby remove delay

    // client related errors
    public static final int ClientInternal       = 500; // Client Internal Error
}
