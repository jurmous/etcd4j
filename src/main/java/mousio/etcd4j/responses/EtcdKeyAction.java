package mousio.etcd4j.responses;

/**
 * The etcd key response actions
 */
public enum EtcdKeyAction {
    set,
    get,
    create,
    update,
    delete,
    expire,
    compareAndSwap,
    compareAndDelete;

    public static EtcdKeyAction[] VALUES = EtcdKeyAction.values();

    public static EtcdKeyAction fromString(String name) {
        for(final EtcdKeyAction action : VALUES) {
            if(action.name().equals(name)) {
                return action;
            }
        }

        throw new IllegalArgumentException("Unable to convert " + name + " to EtcdKeyAction");
    }
}