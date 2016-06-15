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

    public void geoJsonImpl()
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

        assert(coords.size()%3 == 0);
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
        assert(y == 0.0);

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
        assert(coord.size()%3 == 0);
        assert(transformM.size() == 16);

        //every 3 is a coordinate (x,y,z)
        //every 6 is a bounding box (x1,y1,z1) (x2,y2,z2)
        List<Double> transformedCoords = new ArrayList<Double>();
        List<Double> res;

        for (int i = 0; i<coord.size(); i+=3)
        {
            res = matrixMult(coord.get(i), coord.get(i+1), coord.get(i+2), transformM);
            transformedCoords.addAll(res);
        }

        System.out.println(transformedCoords);

        assert(transformedCoords.size() == coord.size());
        return null;
    }

    @Override
    public void getGeoJson(List<Double> pixels)
    {

    }
}
