package mousio.etcd4j.responses;

/**
 * Etcd Store Stats response
 */
public class EtcdStoreStatsResponse {
  private final long compareAndSwapFail;
  private final long compareAndSwapSuccess;
  private final long createFail;
  private final long createSuccess;
  private final long deleteFail;
  private final long deleteSuccess;
  private final long expireCount;
  private final long getsFail;
  private final long getsSuccess;
  private final long setsFail;
  private final long setsSuccess;
  private final long updateFail;
  private final long updateSuccess;
  private final long watchers;

  EtcdStoreStatsResponse(
      long compareAndSwapFail,
      long compareAndSwapSuccess,
      long createFail,
      long createSuccess,
      long deleteFail,
      long deleteSuccess,
      long expireCount,
      long getsFail,
      long getsSuccess,
      long setsFail,
      long setsSuccess,
      long updateFail,
      long updateSuccess,
      long watchers) {

    this.compareAndSwapFail=compareAndSwapFail;
    this.compareAndSwapSuccess=compareAndSwapSuccess;
    this.createFail=createFail;
    this.createSuccess=createSuccess;
    this.deleteFail=deleteFail;
    this.deleteSuccess=deleteSuccess;
    this.expireCount=expireCount;
    this.getsFail=getsFail;
    this.getsSuccess=getsSuccess;
    this.setsFail=setsFail;
    this.setsSuccess=setsSuccess;
    this.updateFail=updateFail;
    this.updateSuccess=updateSuccess;
    this.watchers=watchers;
  }

  public long getCompareAndSwapFail() {
    return compareAndSwapFail;
  }

  public long getCompareAndSwapSuccess() {
    return compareAndSwapSuccess;
  }

  public long getCreateFail() {
    return createFail;
  }

  public long getCreateSuccess() {
    return createSuccess;
  }

  public long getDeleteFail() {
    return deleteFail;
  }

  public long getDeleteSuccess() {
    return deleteSuccess;
  }

  public long getExpireCount() {
    return expireCount;
  }

  public long getGetsFail() {
    return getsFail;
  }

  public long getsSuccess() {
    return getsSuccess;
  }

  public long setsFail() {
    return setsFail;
  }

  public long setsSuccess() {
    return setsSuccess;
  }

  public long updateFail() {
    return updateFail;
  }

  public long updateSuccess() {
    return updateSuccess;
  }

  public long watchers() {
    return watchers;
  }
}
