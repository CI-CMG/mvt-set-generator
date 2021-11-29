package edu.colorado.cires.cmg.mvtset;

import java.util.Objects;

class BoundingBox {

  private final double north;
  private final double south;
  private final double east;
  private final double west;

  BoundingBox(double north, double south, double east, double west) {
    this.north = north;
    this.south = south;
    this.east = east;
    this.west = west;
  }

  double getNorth() {
    return north;
  }

  double getSouth() {
    return south;
  }

  double getEast() {
    return east;
  }

  double getWest() {
    return west;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BoundingBox that = (BoundingBox) o;
    return Double.compare(that.north, north) == 0 && Double.compare(that.south, south) == 0
        && Double.compare(that.east, east) == 0 && Double.compare(that.west, west) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(north, south, east, west);
  }

  @Override
  public String toString() {
    return "BoundingBox{" +
        "north=" + north +
        ", south=" + south +
        ", east=" + east +
        ", west=" + west +
        '}';
  }
}
