package br.com.diego.GridGeom;


import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.DefaultGridFeatureBuilder;
import org.geotools.grid.Grids;
import org.geotools.grid.oblong.Oblongs;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.io.IOException;
import java.io.StringWriter;


@RestController
public class GerarGridImp {

    private final String GEOJSON = "{ \"type\": \"Polygon\", \"coordinates\": [ [ [ -26.01, -8.92 ], [ -33.22, -31.35 ], [ -11.07, -32.39 ], [ -2.46, -11.69 ], [ -26.01, -8.92 ] ] ] }";

    @GetMapping(name = "/geometria", produces= MediaType.APPLICATION_JSON_VALUE)
    public Object gerarGrade(@PathParam("qtde") Double qtde, @PathParam("s1") Double s1, @PathParam("s2") Double s2) throws IOException {

        SimpleFeatureSource grid = null;

        Geometry geom = getGeometriaFromGeoJSON(GEOJSON);

        ReferencedEnvelope referencedEnvelope = JTS.toEnvelope(geom);

        ReferencedEnvelope gridBounds = null;

        if(qtde > 0) {

            if(validarQuadradoPerfeito(qtde)){
                grid = getGridFromQuadrado(qtde, referencedEnvelope);
            }else if(qtde % 2 == 0){
                grid = getGridFromRetangulo(qtde, referencedEnvelope);
            }

        }else {
            grid = getGridEmDistancia(s2, referencedEnvelope);
        }

        SimpleFeatureIterator iterator = grid.getFeatures().features();

        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

        while(iterator.hasNext()){
            SimpleFeature next = iterator.next();
            featureCollection.add(next);
        }

        FeatureJSON fjson = new FeatureJSON();
        StringWriter writer = new StringWriter();
        fjson.writeFeatureCollection(featureCollection, writer);
        String s = writer.toString();

        return s;
    }

    private SimpleFeatureSource getGridEmDistancia(Double distanciaEntreGrids, ReferencedEnvelope referencedEnvelope) {
        ReferencedEnvelope gridBounds = new ReferencedEnvelope(referencedEnvelope, DefaultGeographicCRS.WGS84);
        SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, distanciaEntreGrids);
        return grid;
    }

    private SimpleFeatureSource getGridFromRetangulo(Double qtdePoligonosNoGrid, ReferencedEnvelope referencedEnvelope) {
        ReferencedEnvelope gridBounds = new ReferencedEnvelope(referencedEnvelope, DefaultGeographicCRS.WGS84);
        double distanceEntreColunas = gridBounds.getWidth() /(qtdePoligonosNoGrid / 2);
        double distanciaEntreLinhas = gridBounds.getHeight() / 2;
        SimpleFeatureSource grid = Oblongs.createGrid(gridBounds, distanceEntreColunas, distanciaEntreLinhas, new DefaultGridFeatureBuilder());
        return grid;
    }

    private SimpleFeatureSource getGridFromQuadrado(Double qtdePoligonosNoGrid, ReferencedEnvelope referencedEnvelope) {
        ReferencedEnvelope gridBounds;
        SimpleFeatureSource grid;
        Double[] edges = pontosIniciais(referencedEnvelope);
        gridBounds = new ReferencedEnvelope(edges[1], edges[0], edges[1], edges[0], DefaultGeographicCRS.WGS84);
        qtdePoligonosNoGrid = Math.sqrt(qtdePoligonosNoGrid);
        double colunas = gridBounds.getWidth() / qtdePoligonosNoGrid;
        grid = Grids.createSquareGrid(gridBounds, colunas);
        return grid;
    }

    private Geometry getGeometriaFromGeoJSON(String geojson) throws IOException {
        GeometryJSON geoJSON = new GeometryJSON(2);
        Geometry geom = geoJSON.read(geojson);
        geom.setSRID(3857);
        return geom;
    }

    private boolean validarQuadradoPerfeito(double qtde){
        double sqrt = Math.sqrt(qtde);
        int x = (int) sqrt;
        return (Math.pow(sqrt,2) == Math.pow(x,2));
    }

    private Double[] pontosIniciais(Envelope referencedEnvelope){
        Double[] edges = new Double[2];

        if(referencedEnvelope.getMaxX() > referencedEnvelope.getMaxY()){
            edges[0] = referencedEnvelope.getMaxX();
        }else{
            edges[0] = referencedEnvelope.getMaxY();
        }

        if(referencedEnvelope.getMinX() < referencedEnvelope.getMinY()){
            edges[1] = referencedEnvelope.getMinX();
        }else{
            edges[1] = referencedEnvelope.getMinY();
        }
        return edges;
    }


}
