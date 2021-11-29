package edu.colorado.cires.cmg.mvtset;

/**
 * Operations for getting, creating, and deleting a MVT set
 */
public interface MvtStore {


  /**
   * Returns the bytes of a MVT for a given index in the form of 'z/x/y'. Returns an empty array if the MVT does not exist.
   *
   * @param index an index in the form of 'z/x/y'
   * @return the bytes of the MVT or an empty array if the MVT does not exist
   */
  byte[] getMvt(String index);

  /**
   * Writes the MVT bytes to the store for the provided index in the form of 'z/x/y'.  MVT should be overwritten if it exists.
   *
   * @param index an index in the form of 'z/x/y'
   * @param mvtBytes the bytes of the MVT
   */
  void saveMvt(String index, byte[] mvtBytes);

  /**
   * Clears all tiles in the store.
   */
  void clearStore();

}
