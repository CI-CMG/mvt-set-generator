package edu.colorado.cires.cmg.mvtset;

import java.util.Map;
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;

public class GeometryDetails {

    private final Geometry geometry;
    private final Map<String, Object> properties;

    public GeometryDetails(Geometry geometry, Map<String, Object> properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeometryDetails that = (GeometryDetails) o;
        return Objects.equals(geometry, that.geometry) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geometry, properties);
    }
}
