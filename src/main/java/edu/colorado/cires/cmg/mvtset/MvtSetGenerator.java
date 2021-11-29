package edu.colorado.cires.cmg.mvtset;


import edu.colorado.cires.cmg.mvt.VectorTile;
import edu.colorado.cires.cmg.mvt.VectorTile.Tile;
import edu.colorado.cires.cmg.mvt.adapt.jts.IGeometryFilter;
import edu.colorado.cires.cmg.mvt.adapt.jts.JtsAdapter;
import edu.colorado.cires.cmg.mvt.adapt.jts.MvtEncoder;
import edu.colorado.cires.cmg.mvt.adapt.jts.MvtReader;
import edu.colorado.cires.cmg.mvt.adapt.jts.TagKeyValueMapConverter;
import edu.colorado.cires.cmg.mvt.adapt.jts.TileGeomResult;
import edu.colorado.cires.cmg.mvt.adapt.jts.UserDataKeyValueMapConverter;
import edu.colorado.cires.cmg.mvt.adapt.jts.model.JtsLayer;
import edu.colorado.cires.cmg.mvt.adapt.jts.model.JtsMvt;
import edu.colorado.cires.cmg.mvt.build.MvtLayerBuild;
import edu.colorado.cires.cmg.mvt.build.MvtLayerParams;
import edu.colorado.cires.cmg.mvt.build.MvtLayerProps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MvtSetGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(MvtSetGenerator.class);
  private static final double CLIP_BORDER_MULTIPLIER = 0.1;
  public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
  private static final IGeometryFilter ACCEPT_ALL = g -> true;

  private final int maxZoom;
  private final double minSimplification;
  private final double simplificationStep;
  private final MvtStore mvtStore;
  private final GeometrySource geometrySource;

  public MvtSetGenerator(
      int maxZoom,
      double minSimplification,
      double maxSimplification,
      MvtStore mvtStore,
      GeometrySource geometrySource) {
    this.maxZoom = maxZoom;
    this.minSimplification = minSimplification;
    this.mvtStore = mvtStore;
    this.geometrySource = geometrySource;
    simplificationStep = (maxSimplification - minSimplification) / maxZoom;
  }

  private static JtsMvt loadMvt(byte[] inBytes) {
    try {
      return MvtReader.loadMvt(
          new ByteArrayInputStream(inBytes),
          GEOMETRY_FACTORY,
          new TagKeyValueMapConverter(),
          MvtReader.RING_CLASSIFIER_V1);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load MVT", e);
    }
  }


  public void deleteTilePyramid() {
    mvtStore.clearStore();
  }

  public void updateTilePyramidLayer(String layerName) {

    try (Stream<GeometryDetails> geometryStream = geometrySource.streamGeometries()) {
      geometryStream.forEach(geometryDetails -> {
        final Map<String, Object> properties = geometryDetails.getProperties();

        Geometry area = geometryDetails.getGeometry();
        double simplification = minSimplification;
        for (int z = maxZoom; z >= 0; z--) {
          System.out.println("simplification " + simplification);

          area = DouglasPeuckerSimplifier.simplify(area, simplification);
          area.setUserData(properties);

          Envelope envelope = area.getEnvelopeInternal();
          String sw = TileCalculator.getTileNumber(envelope.getMinX(), envelope.getMinY(), z);
          String ne = TileCalculator.getTileNumber(envelope.getMaxX(), envelope.getMaxY(), z);
          int minXTile = Integer.parseInt(sw.split("/")[1]);
          int maxXTile = Integer.parseInt(ne.split("/")[1]);
          int maxYTTile = Integer.parseInt(sw.split("/")[2]);
          int minYTTile = Integer.parseInt(ne.split("/")[2]);
          for (int x = minXTile; x <= maxXTile; x++) {
            for (int y = minYTTile; y <= maxYTTile; y++) {
              String tileIndex = z + "/" + x + "/" + y;

              BoundingBox box = TileCalculator.tile2boundingBox(x, y, z);
              Envelope tileEnvelope = new Envelope(box.getWest(), box.getEast(), box.getSouth(), box.getNorth());

              Envelope clipEnvelope = new Envelope(tileEnvelope);
              clipEnvelope.expandBy((box.getEast() - box.getWest()) * CLIP_BORDER_MULTIPLIER,
                  (box.getNorth() - box.getSouth()) * CLIP_BORDER_MULTIPLIER);

              TileGeomResult tileGeom = JtsAdapter.createTileGeom(
                  Collections.singletonList(area),
                  tileEnvelope,
                  clipEnvelope,
                  GEOMETRY_FACTORY,
                  new MvtLayerParams(),
                  ACCEPT_ALL);

              byte[] inBytes = mvtStore.getMvt(tileIndex);
              byte[] outBytes;
              if (inBytes != null && inBytes.length > 0) {
                outBytes = updateMvt(inBytes, layerName, tileGeom);
              } else {
                outBytes = createNewMvt(layerName, tileGeom);
              }

              mvtStore.saveMvt(tileIndex, outBytes);

              System.out.println("wrote " + layerName + " - " + tileIndex);
              LOGGER.info("wrote {}-{}", layerName, tileIndex);

            }
          }
          simplification += simplificationStep;
        }

      });
    }
  }

  private byte[] updateMvt(byte[] inBytes, String layerName, TileGeomResult tileGeom) {
    JtsMvt mvt = loadMvt(inBytes);
    List<JtsLayer> layers = new ArrayList<>(mvt.getLayers());
    JtsLayer layer = layers.stream().filter(l -> l.getName().equals(layerName))
        .findFirst().orElse(null);
    List<Geometry> geoms = new ArrayList<>(tileGeom.mvtGeoms);
    if (layer == null) {
      layer = new JtsLayer(layerName, geoms);
      layers.add(layer);
    } else {
      layers.remove(layer);
      geoms.addAll(layer.getGeometries());
      layer = new JtsLayer(layerName, geoms);
    }
    layers.add(layer);
    return MvtEncoder.encode(new JtsMvt(layers));
  }

  private byte[] createNewMvt(String layerName, TileGeomResult tileGeom) {
    return encodeMvt(layerName, tileGeom).toByteArray();
  }

  private static Tile encodeMvt(String layerName, TileGeomResult tileGeom) {
    MvtLayerProps layerProps = new MvtLayerProps();
    List<Tile.Feature> features = JtsAdapter.toFeatures(tileGeom.mvtGeoms, layerProps, new UserDataKeyValueMapConverter());
    Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, new MvtLayerParams()).addAllFeatures(features);
    MvtLayerBuild.writeProps(layerBuilder, layerProps);
    Tile.Layer layer = layerBuilder.build();
    return VectorTile.Tile.newBuilder().addLayers(layer).build();
  }

}
