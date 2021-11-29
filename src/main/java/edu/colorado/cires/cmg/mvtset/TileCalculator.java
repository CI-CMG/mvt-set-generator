package edu.colorado.cires.cmg.mvtset;

final class TileCalculator {

  static BoundingBox tile2boundingBox(final int x, final int y, final int zoom) {
    double north = tile2lat(y, zoom);
    double south = tile2lat(y + 1, zoom);
    double west = tile2lon(x, zoom);
    double east = tile2lon(x + 1, zoom);
    return new BoundingBox(north, south, east, west);
  }

  static String getTileNumber(final double lon, final double lat, final int zoom) {
    int twoToZoom = 1 << zoom; //Math.pow(2, zoom)
    int numXTiles = 2 * twoToZoom;
    int numYTiles = numXTiles / 2;
    int xtile = (int) Math.floor(((lon + 180) / 360d) * numXTiles);
    int ytile = (int) Math.floor(numYTiles - (((lat + 90) / 180d) * numYTiles));

    if (xtile < 0) {
      xtile = 0;
    }
    if (xtile >= numXTiles) {
      xtile = numXTiles - 1;
    }
    if (ytile < 0) {
      ytile = 0;
    }
    if (ytile >= numYTiles) {
      ytile = numYTiles - 1;
    }
    return ("" + zoom + "/" + xtile + "/" + ytile);
  }

  static double tile2lon(int x, int z) {
    int twoToZoom = 1 << z; //Math.pow(2, zoom)
    int numXTiles = 2 * twoToZoom;
    double degreesPerTile = 360d / numXTiles;
    return (x * degreesPerTile) - 180d;
  }

  static double tile2lat(int y, int z) {
    int twoToZoom = 1 << z; //Math.pow(2, zoom)
    int numXTiles = 2 * twoToZoom;
    int numYTiles = numXTiles / 2;
    double degreesPerTile = 180d / numYTiles;
    return 90d - (y * degreesPerTile);
  }

  private TileCalculator() {

  }

}
