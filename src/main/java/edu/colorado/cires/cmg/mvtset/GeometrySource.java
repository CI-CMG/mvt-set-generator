package edu.colorado.cires.cmg.mvtset;

import java.util.stream.Stream;

public interface GeometrySource {

  Stream<GeometryDetails> streamGeometries();

}
