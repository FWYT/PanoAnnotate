import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vagrant on 6/15/16.
 */
public class clientApplication {

    public static void main(String[] args)
    {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(geoJsonService.class).to(geoJsonImpl.class);
            }
        });

        geoJsonImpl geo = injector.getInstance(geoJsonImpl.class);
        geo.geoJsonImpl();
        List<Double> r1 = geo.getCoordinates("walmart/0339/A/2/71b6bcdb36fc86f4a4edb35bc2cf8b36/pano.tif");
        List<Double> r2 = geo.getTransformMatrix("walmart/0894/A/16/9c54ddc034bf5157d95b93ae62c8f12c/pano.tif");
        //List<Double> trans = Arrays.asList(1.0,1.0,2.0,1.0,2.0,1.0,1.0,1.0,1.0,2.0,1.0,1.0,1.0,1.0,1.0,2.0);
        //geo.matrixMult(2.0,0.0,4.0, trans);
        geo.transformCoordinates(r1,r2);
    }
}
