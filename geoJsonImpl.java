import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.geojson.GeometryCollection;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.postgresql.ds.PGPoolingDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by vagrant on 6/15/16.
 */
public class geoJsonImpl implements geoJsonService {

    private static PGPoolingDataSource source;
    private static Connection c;

    @Inject
    public geoJsonImpl()
    {
        System.out.println("Start");
        source = new PGPoolingDataSource();
        source.setDataSourceName("db_source");
        source.setServerName("localhost");
        source.setDatabaseName("customer_db");
        source.setUser("postgres");
        source.setPassword("postgres");
        try {
            c = source.getConnection();
            System.out.println("Connected");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    @Override
    public String getGeoJson(List<Double> pixels)
    {
        return null;
    }

    @Override
    public List<Double> getCoordinates(String imagePath)
    {
        //list of lists or single list where every 6 is a square?
        //ArrayList<ArrayList<Double>> coords = new ArrayList<ArrayList<Double>>();
        List<Double> coords = new ArrayList<Double>();
        String sql = "select x1,y1,z1,x2,y2,z2 from bnr.bnr_out_read where image = '" + imagePath + "'";

        try {
            Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery(sql);

            while (res.next()) {
                coords.add(res.getDouble("x1"));
                coords.add(res.getDouble("y1"));
                coords.add(res.getDouble("z1"));
                coords.add(res.getDouble("x2"));
                coords.add(res.getDouble("y2"));
                coords.add(res.getDouble("z2"));
            }

            res.close();
            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }

        System.out.println(coords);
        assert(coords.size()%6 == 0);
        return coords;
    }

    @Override
    public List<Double> getTransformMatrix(String imagePath)
    {
        List<Double> transform = null;
        String sql = "select transform_array from bnr.bnr_image where path = '" + imagePath + "'";

        try {
            Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery(sql);

            while (res.next()) {
                transform = Arrays.asList((Double[])res.getArray("transform_array").getArray());
            }

            System.out.println(transform);
            /*for (int i = 0; i< transform.length; i++) {
                System.out.print(transform[i] + "\n");
            }*/

            res.close();
            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }

        assert(transform.size() == 16);
        return transform;
    }

    private List<Double> matrixMult(double x, double y, double z, List<Double> transform)
    {
        assert(transform.size() == 16);

        List<Double> result = new ArrayList<Double>();
        Double m1,m2,m3,m4;

        //4x4 matrix, coor = [x,y,z,1].
        //Transformation = transform * coor
        for (int i = 0; i<transform.size(); i+=4)
        {
            m1 = transform.get(i) * x;
            m2 = transform.get(i+1) * y;
            m3 = transform.get(i+2) * z;
            m4 = transform.get(i+3) * 1;
            result.add(m1+m2+m3+m4);
        }

        assert(result.size() == 4);
        //assert(result.get(3) == 1);

        //System.out.println(result);

        //last entry will be 1 always
        return result.subList(0,3);
    }

    @Override
    public List<Double> transformCoordinates(List<Double> coord, List<Double> transformM)
    {
        assert(coord.size()%6 == 0);
        assert(transformM.size() == 16);

        //every 3 is a coordinate (x,y,z)
        //every 6 is a bounding box (x1,y1,z1) (x2,y2,z2)
        List<Double> transformedCoords = new ArrayList<Double>();
        List<Double> res = null;


        for (int i = 0; i<coord.size(); i+=3)
        {
            //only entries (1,1) and (2,1) are needed from the resulting matrix
            res = matrixMult(coord.get(i), coord.get(i+1), coord.get(i+2), transformM);
            transformedCoords.addAll(res.subList(0,2));
        }

        System.out.println(transformedCoords);


        assert(transformedCoords.size() % 2 == 0);
        return transformedCoords;
    }


    public String parseGeoJson(List<Double> pixels)
    {
        ObjectMapper mapper = new ObjectMapper();
        GeometryCollection gc = new GeometryCollection();
        Polygon p ;
        String json = "";

        Double x1,y1,x2,y2,x3,y3,x4,y4,width,height;

        //every 4 points represent 2 corners of a bounding box
        for (int i = 0; i<pixels.size(); i+=4)
        {
            //left top
            x1 = pixels.get(i);
            y1 = pixels.get(i+1);
            //right bottom
            x2 = pixels.get(i+2);
            y2 = pixels.get(i+3);

            //left top corner and right bottom corner
            width = x2-x1;
            height = y1-y2;

            //right top corner
            x3 = x1+width;
            y3 = y1;

            //left bottom
            x4 = x2-width;
            y4 = y2;

            //add to geometry collection
            p = new Polygon(new LngLatAlt(x4,y4), new LngLatAlt(x1,y1), new LngLatAlt(x3,y3), new LngLatAlt(x2,y2));
            gc.add(p);
        }

        try {
            json = mapper.writeValueAsString(gc);
            System.out.println("\n");
            System.out.println(json);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }

        return json;
    }

    public String getGeoJson2 (String image)
    {
        List<Double> coords = getCoordinates(image);
        List<Double> transformMatrix = getTransformMatrix(image);
        List<Double> pixels = transformCoordinates(coords, transformMatrix);
        return parseGeoJson(pixels);
    }
}
