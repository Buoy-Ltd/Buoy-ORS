package org.heigit.ors.routing.graphhopper.extensions.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import com.vividsolutions.jts.geom.*;
import org.heigit.ors.api.errors.GenericErrorCodes;
import org.heigit.ors.api.requests.routing.RequestProfileParamsWeightings;
import org.heigit.ors.exceptions.ParameterValueException;
import org.heigit.ors.exceptions.StatusCodeException;
import org.heigit.ors.geojson.GeometryJSON;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.List;
import static com.graphhopper.util.PointList.round6;

/**
 * Decreases the weight for a certain set of edges by a given factor and thus makes them more likely to be part of
 * a shorter path
 *
 * @author Sam Stenner
 */
public class PreferPolygonsWeighting extends FastestWeighting {

    private Polygon[] polygons;
    private Double weight = 1.0;

    public PreferPolygonsWeighting(FlagEncoder encoder, PMap map) {
        super(encoder, map);
        String json = map.get("polygons", "{}");
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(json);
            polygons = convertPolygons(obj);
            weight = convertWeight(obj, weight);
        } catch (ParseException | StatusCodeException e) {
            e.printStackTrace();
        }
    }

    private Double convertWeight(JSONObject obj, Double _default) {
        Double param = (Double) obj.get("weight");
        return param != null ? param : _default;
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeIteratorState, boolean reverse, int prevOrNextEdgeId, long time) {
        for (Polygon polygon : polygons) {
            PointList list = edgeIteratorState.fetchWayGeometry(3);
            LineString string = toLineString(list);
            if (polygon.contains(string) || polygon.intersects(string)) {
                return 0.0;
            }
        }
        return weight;
    }

    public LineString toLineString(PointList list) {
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[list.getSize() == 1 ? 2 : list.getSize()];
        for(int i = 0; i < list.getSize(); ++i) {
            coordinates[i] = new Coordinate(round6(list.getLongitude(i)), round6(list.getLatitude(i)));
        }
        if (list.getSize() == 1) {
            coordinates[1] = coordinates[0];
        }
        return gf.createLineString(coordinates);
    }

    @SuppressWarnings("unchecked")
    protected Polygon[] convertPolygons(JSONObject geoJson) throws StatusCodeException {
        org.json.JSONObject complexJson = new org.json.JSONObject();
        complexJson.put("type", geoJson.get("type"));
        List<List<Double[]>> coordinates = (List<List<Double[]>>) geoJson.get("coordinates");
        complexJson.put("coordinates", coordinates);
        Geometry convertedGeom;
        try {
            convertedGeom = GeometryJSON.parse(complexJson);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParameterValueException(GenericErrorCodes.INVALID_JSON_FORMAT, RequestProfileParamsWeightings.PARAM_PREFER_POLYGONS);
        }
        Polygon[] polygons;
        if (convertedGeom instanceof Polygon) {
            polygons = new Polygon[]{(Polygon) convertedGeom};
        } else if (convertedGeom instanceof MultiPolygon) {
            MultiPolygon multiPoly = (MultiPolygon) convertedGeom;
            polygons = new Polygon[multiPoly.getNumGeometries()];
            for (int i = 0; i < multiPoly.getNumGeometries(); i++)
                polygons[i] = (Polygon) multiPoly.getGeometryN(i);
        } else {
            throw new ParameterValueException(GenericErrorCodes.INVALID_PARAMETER_VALUE, RequestProfileParamsWeightings.PARAM_PREFER_POLYGONS);
        }
        return polygons;
    }

    @Override
    public String getName() {
        return "prefer_polygons";
    }

}
