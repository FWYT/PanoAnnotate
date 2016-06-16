import java.util.ArrayList;
import java.util.List;

/**
 * Created by vagrant on 6/15/16.
 */
public interface geoJsonService {

    List<Double> getCoordinates(String imagePath);
    List<Double> getTransformMatrix(String imagePath);
    List<Double> transformCoordinates(List<Double> coord, List<Double> transformM);
    String getGeoJson(List<Double> pixels);

}


